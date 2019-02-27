package org.apache.ibatis.demo.util;

import ognl.Ognl;
import ognl.OgnlException;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.demo.mapper.MailMapper;
import org.apache.ibatis.demo.pojo.Mail;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.StringTokenizer;

/**
 * Created by QDHL on 2018/10/19.
 *
 * @author mingqiang ji
 */
public class Main {

    public static void main(String[] args) throws NoSuchMethodException {
        Method paramTest = MailMapper.class.getMethod("paramTest", new Class[]{Integer.class,Integer.class,String.class});
        Annotation[][] parameterAnnotations = paramTest.getParameterAnnotations();
        System.out.println(parameterAnnotations.length);
        int count = parameterAnnotations.length;

        for (int i = 0; i < count; i++) {
            for (Annotation annotation : parameterAnnotations[i]) {
                if (annotation instanceof Param) {

                }
            }

        }


    }
}
