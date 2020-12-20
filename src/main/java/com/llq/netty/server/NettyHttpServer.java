package com.llq.netty.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class NettyHttpServer {

    private static int port;
    private final static Properties pro = new Properties();
    static {
        InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("application.properties");
        if (resourceAsStream == null) {
            try {
                throw new Exception("找不到对应的application.properties配置文件");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            pro.load(resourceAsStream);
            String portProperty = pro.getProperty("server.port");
            if (null == portProperty || portProperty.equals("") || !isDigits(portProperty) ) {
                throw new Exception("端口号配置异常");
            }
            port = Integer.getInteger(portProperty);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean isDigits(String str) {
        char[] chars = str.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (!Character.isDigit(chars[i])) {
                return false;
            }
        }
        return true;
    }

}
