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
package org.apache.ibatis.demo;

import org.apache.ibatis.demo.mapper.MailMapper;
import org.apache.ibatis.demo.pojo.Mail;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by QDHL on 2018/10/13.
 *
 * @author mingqiang ji
 */
public class Mybatis {
    public static void main(String[] args) {
        String resource = "org/apache/ibatis/demo/config.xml";
        Reader reader;
        try {
            reader = Resources.getResourceAsReader(resource);
            SqlSessionFactory sqlMapper = new SqlSessionFactoryBuilder().build(reader);

            SqlSession session = sqlMapper.openSession();
            try {
                MailMapper mapper = session.getMapper(MailMapper.class);
//                Mail mail = mapper.selectMailById(1);
//                System.out.println(mail.getMail());
                ArrayList<Integer> ids = new ArrayList<>();
                ids.add(1);
                ids.add(2);
                mapper.getSubjectList(ids);
               /* Mail mail1 = new Mail();
                mail1.setMail("11111");
                mail1.setWebId(2);
                mail1.setCreateTime(new Date());
                mapper.insertMail(mail1);
                System.out.println(mail1.getId());*/
               /* Mail mail = (Mail) session.selectOne("org.apache.ibatis.demo.mapper.MailMapper.selectMailById", 1);
                System.out.println(mail.getWebId());*/


            } finally {
                session.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
