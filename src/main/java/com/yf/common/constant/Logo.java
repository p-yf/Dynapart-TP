package com.yf.common.constant;

import lombok.extern.slf4j.Slf4j;

import java.lang.ref.SoftReference;

/**
 * @author yyf
 * @date 2025/9/19 11:03
 * @description 使用软引用管理的Logo常量类(过度设计了 ha~ha~ （>_<） )
 */
@Slf4j
public class Logo {
    //禁止实例化
    private Logo() {
    }

    private static SoftReference<StringBuilder> START_LOGO = new SoftReference<>(
            new StringBuilder("\033[34m")
                    .append("""
               |==============================╔═══════════════════════════════════════╗==============================|
               l    e    t    '    s    *    *  w ██████╗     ████████╗    ██████╗  w  *    b    e    c    o    m    e
               |==============================║   ██╔══██╗    ╚══██╔══╝    ██╔══██╗   ║==============================|
                                              ║   ██║  ██║   *   ██║   *   ██████╔╝   ║
               |==============================║   ██║  ██║       ██║       ██╔═══╝    ║==============================|
               b    e    t    t    e    r    *  w ██████╔╝   *   ██║   *   ██║      w  *    *    a    n    d    *    *
               |==============================║   ╚═════╝        ╚═╝       ╚═╝        ║==============================|
                                              ║        dynamic high-performance       ║
               |==============================║            * thread pool *            ║==============================|
               b    e    t    t    e    r    *  w           written by yf           w  *    ——   @    y    y    f    *
               |==============================╚═══════════════════════════════════════╝==============================|
                                                         FASTER AND STABLER
               """ )
                    .append("\033[0m")
                    );

    public static final String LOG_LOGO = "\033[34mYF:D--T--P===>\033[0m";

    public static String startLogo() {
        StringBuilder logo = START_LOGO.get();
        if(logo==null){
            //清除引用
            START_LOGO=null;
            log.info(LOG_LOGO+"START_LOGO已经被清除，如果还有使用则被替换成了："+LOG_LOGO);
            return LOG_LOGO;
        }
        return logo.toString();
    }

}
