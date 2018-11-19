package org.apache.ibatis.demo.util;

import ognl.Ognl;
import ognl.OgnlException;
import org.apache.ibatis.demo.pojo.Mail;

import java.util.StringTokenizer;

/**
 * Created by QDHL on 2018/10/19.
 *
 * @author mingqiang ji
 */
public class Main {

    public static void main(String[] args) {
        Mail user = new Mail();
        user.setWebId(11);
        user.setMail("2222");
        try
        {
            System.out.println(Ognl.getValue("webId", user,user.getClass()));
            System.out.println(Ognl.getValue("mail", user,user.getClass()));

            //输出结果：
            //rcx
            //com.rcx.ognl.Address@dda25b
            //110003
        }
        catch (OgnlException e)
        {
            e.printStackTrace();
        }
    }
}
