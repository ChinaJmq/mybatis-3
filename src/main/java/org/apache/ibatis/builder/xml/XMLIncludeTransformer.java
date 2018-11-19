/**
 *    Copyright 2009-2018 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.builder.xml;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.builder.IncompleteElementException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.parsing.PropertyParser;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.session.Configuration;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Frank D. Martinez [mnesarco]
 * XML <include /> 标签的转换器，负责将 SQL 中的 <include /> 标签转换成对应的 <sql /> 的内容
 */
public class XMLIncludeTransformer {

  private final Configuration configuration;
  private final MapperBuilderAssistant builderAssistant;

  public XMLIncludeTransformer(Configuration configuration, MapperBuilderAssistant builderAssistant) {
    this.configuration = configuration;
    this.builderAssistant = builderAssistant;
  }

  public void applyIncludes(Node source) {
    Properties variablesContext = new Properties();
    Properties configurationVariables = configuration.getVariables();
    if (configurationVariables != null) {
      // 复制一份configuration对象中的全局变量
      variablesContext.putAll(configurationVariables);
    }
    /* 应用<include/>包含的<sql/>片段 */
    applyIncludes(source, variablesContext, false);
  }

  /**
   * Recursively apply includes through all SQL fragments.
   * @param source Include node in DOM tree
   * @param variablesContext Current context for static variables with values
   */
  private void applyIncludes(Node source, final Properties variablesContext, boolean included) {
    System.out.println(source.getNodeName());
    // 如果是<include/>标签
    if (source.getNodeName().equals("include")) {
      /* 提取refid对应的sql片段的Node */
      Node toInclude = findSqlFragment(getStringAttribute(source, "refid"), variablesContext);
      /* 从节点定义中读取占位符和它们的值,也就是获取<property/>标签的值 */
      Properties toIncludeContext = getVariablesContext(source, variablesContext);
      // 将包含的sql片段节点作为参数递归此方法，也就是处理<sql/>,主要是替换占位符
      //如以前的sql为 ${alias}.id,  ${alias}.create_time,  ${alias}.modify_time,  ${alias}.web_id,  ${alias}.mail, use_for
      //之后会变为 t.id,  t.create_time,  t.modify_time,  t.web_id,  t.mail, use_for
      applyIncludes(toInclude, toIncludeContext, true);

      //-----------------以下的操作是将sql解析成数据库中能识别的sql----------------
      /**
       * 如之前
       *  select
       *         <include refid="fieldAlias" >
       *         <property name="alias" value="t" />
       *         </include>
       *         from mail t;
       *  之后
       *   select
       *         t.id,  t.create_time,  t.modify_time,  t.web_id,  t.mail, use_for
       *         from mail t;
       */

      // 如果当前节点和被包含节点不在同一个文档中，则将被包含节点导入当前节点所在的文档中
      if (toInclude.getOwnerDocument() != source.getOwnerDocument()) {
        toInclude = source.getOwnerDocument().importNode(toInclude, true);
      }
      // 将当前节点替换成被包含节点,即source替换成toInclude,也就是将<include/>标签替换成<sql/>标签
      source.getParentNode().replaceChild(toInclude, source);
      while (toInclude.hasChildNodes()) {
        // 将子节点放在被包含节点的前面，也就是将t.id,  t.create_time,  t.modify_time,  t.web_id,  t.mail, use_for
        //放到<sql/>标签的前边
        toInclude.getParentNode().insertBefore(toInclude.getFirstChild(), toInclude);
      }
      // 从父节点中移除被包含节点，也就是最后删除<sql/>标签
      toInclude.getParentNode().removeChild(toInclude);
      //元素，如<select/><insert/><sql/>
    } else if (source.getNodeType() == Node.ELEMENT_NODE) {
      if (included && !variablesContext.isEmpty()) {
        // replace variables in attribute values
        //替换掉节点中属性用占位符表示的
        NamedNodeMap attributes = source.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
          Node attr = attributes.item(i);
          attr.setNodeValue(PropertyParser.parse(attr.getNodeValue(), variablesContext));
        }
      }
      //遍历子节点递归此方法
      NodeList children = source.getChildNodes();
      for (int i = 0; i < children.getLength(); i++) {
        applyIncludes(children.item(i), variablesContext, included);
      }
      //文本类型的Node，主要处理<sql/>标签中的占位符
      //如${alias}.id,  ${alias}.create_time,  ${alias}.modify_time,  ${alias}.web_id,  ${alias}.mail, use_for
      //变成t.id,  t.create_time,  t.modify_time,  t.web_id,  t.mail, use_for
    } else if (included && source.getNodeType() == Node.TEXT_NODE
        && !variablesContext.isEmpty()) {
      // replace variables in text node
      // 如果是文本类型的Node，则解析并设置节点值，主要是替换节点值中声明的变量如${var1}
      source.setNodeValue(PropertyParser.parse(source.getNodeValue(), variablesContext));
    }
  }

  private Node findSqlFragment(String refid, Properties variables) {
    // 解析refid中声明的变量，如${var1}
    refid = PropertyParser.parse(refid, variables);
    // 应用当前命名空间，命名空间+refid
    refid = builderAssistant.applyCurrentNamespace(refid, true);
    try {
      // 根据refid从之前解析<sql/>标签时添加的映射中获取对应的节点
      XNode nodeToInclude = configuration.getSqlFragments().get(refid);
      //克隆节点，true表示复制的节点是包括原节点的所有属性和子节点
      return nodeToInclude.getNode().cloneNode(true);
    } catch (IllegalArgumentException e) {
      throw new IncompleteElementException("Could not find SQL statement to include with refid '" + refid + "'", e);
    }
  }

  /**
   * 获取节点的属性值
   * @param node
   * @param name
   * @return
   */
  private String getStringAttribute(Node node, String name) {
    return node.getAttributes().getNamedItem(name).getNodeValue();
  }

  /**
   * Read placeholders and their values from include node definition. 
   * @param node Include node instance
   * @param inheritedVariablesContext Current context used for replace variables in new variables values
   * @return variables context from include instance (no inherited values)
   */
  private Properties getVariablesContext(Node node, Properties inheritedVariablesContext) {
    Map<String, String> declaredProperties = null;
    //所有的子节点，也就是<property/>标签
    NodeList children = node.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node n = children.item(i);
      if (n.getNodeType() == Node.ELEMENT_NODE) {
        // 提取name属性
        String name = getStringAttribute(n, "name");
        // Replace variables inside
        // 提取value属性，并解析属性值中声明的变量
        String value = PropertyParser.parse(getStringAttribute(n, "value"), inheritedVariablesContext);
        if (declaredProperties == null) {
          declaredProperties = new HashMap<>();
        }
        // 添加到声明的属性映射中
        if (declaredProperties.put(name, value) != null) {
          // 重复声明抛异常
          throw new BuilderException("Variable " + name + " defined twice in the same include definition");
        }
      }
    }
    if (declaredProperties == null) {
      // 声明属性为空直接返回继承的全局变量
      return inheritedVariablesContext;
    } else {
      // 声明属性不为空则将两者放入新的Properties对象中返回
      Properties newProperties = new Properties();
      newProperties.putAll(inheritedVariablesContext);
      newProperties.putAll(declaredProperties);
      return newProperties;
    }
  }
}
