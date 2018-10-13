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
package org.apache.ibatis.demo.mapper;

import org.apache.ibatis.demo.pojo.Mail;

import java.util.List;

/**
 * Created by QDHL on 2018/10/13.
 *
 * @author mingqiang ji
 */
public interface MailMapper {
    /**
     * 插入一条邮箱信息
     */
    public long insertMail(Mail mail);

    /**
     * 删除一条邮箱信息
     */
    public int deleteMail(long id);

    /**
     * 更新一条邮箱信息
     */
    public int updateMail(Mail mail);

    /**
     * 查询邮箱列表
     */
    public List<Mail> selectMailList();

    /**
     * 根据主键id查询一条邮箱信息
     */
    public Mail selectMailById(long id);

}


