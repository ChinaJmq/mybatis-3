/**
 *    Copyright 2009-2017 the original author or authors.
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
package org.apache.ibatis.parsing;

/**
 * @author Clinton Begin
 *
 * 通用的 Token 解析器
 */
public class GenericTokenParser {
    /**
     * 开始的 Token 字符串
     */
  private final String openToken;
    /**
     * 结束的 Token 字符串
     */
  private final String closeToken;
  private final TokenHandler handler;

  public GenericTokenParser(String openToken, String closeToken, TokenHandler handler) {
    this.openToken = openToken;
    this.closeToken = closeToken;
    this.handler = handler;
  }

    /**
     * 主要为了解析以 openToken 开始，以 closeToken 结束的 Token ，并提交给 handler 进行处理，即 <x> 处。
     *
     * 基本的逻辑是在这里parse方法实现的,具体的解析由不同的TokenHandler处理特定的逻辑，这也是为什么 GenericTokenParser 叫做通用的原因
     * @param text
     * @return
     */
  public String parse(String text) {
    // 文本为空返回空字符串
    if (text == null || text.isEmpty()) {
      return "";
    }
    // search open token
    // openToken在文本中不存在，返回文本
    int start = text.indexOf(openToken, 0);
    if (start == -1) {
      return text;
    }
    char[] src = text.toCharArray();
    //偏移量
    int offset = 0;
    final StringBuilder builder = new StringBuilder();
    // 匹配到 openToken 和 closeToken 之间的表达式
    StringBuilder expression = null;
    while (start > -1) {
      // 如果openToken前面是反斜杠\转义字符，移除掉并继续
      if (start > 0 && src[start - 1] == '\\') {
        // this open token is escaped. remove the backslash and continue.
          // 添加 [offset, start - offset - 1] 和 openToken 的内容，添加到 builder 中
        builder.append(src, offset, start - offset - 1).append(openToken);
        //更新偏移量
        offset = start + openToken.length();
      } else {
        // found open token. let's search close token.
        // 找到了openToken，下面要找closeToken
        // 创建 expression 对象
        if (expression == null) {
          expression = new StringBuilder();
        } else { // 重置 expression 对象
          expression.setLength(0);
        }
        // 添加 offset 和 openToken 之间的内容，添加到 builder 中
        builder.append(src, offset, start - offset);
          // 修改 offset
        offset = start + openToken.length();
          // 从offset位置寻找结束的 closeToken 的位置
        int end = text.indexOf(closeToken, offset);
        while (end > -1) {
          if (end > offset && src[end - 1] == '\\') {
            // this close token is escaped. remove the backslash and continue.
              // 因为 endToken 前面一个位置是 \ 转义字符，所以忽略 \
              // 添加 [offset, end - offset - 1] 和 endToken 的内容，添加到 builder 中
            expression.append(src, offset, end - offset - 1).append(closeToken);
              // 修改 offset
            offset = end + closeToken.length();
              // 继续，寻找结束的 closeToken 的位置
            end = text.indexOf(closeToken, offset);
          } else {
              // 添加 [offset, end - offset] 的内容，添加到 builder 中
            expression.append(src, offset, end - offset);
              // 修改 offset
            offset = end + closeToken.length();
            break;
          }
        }
          // 拼接内容
        if (end == -1) {
          // close token was not found.
            // closeToken 未找到，直接拼接
          builder.append(src, start, src.length - start);
          offset = src.length;
        } else {
            // <x> closeToken 找到，将 expression 提交给 handler 处理 ，并将处理结果添加到 builder 中
          builder.append(handler.handleToken(expression.toString()));
            // 修改 offset
          offset = end + closeToken.length();
        }
      }
        // 继续，寻找开始的 openToken 的位置
      start = text.indexOf(openToken, offset);
    }
      // 拼接剩余的部分
    if (offset < src.length) {
      builder.append(src, offset, src.length - offset);
    }
    return builder.toString();
  }
}
