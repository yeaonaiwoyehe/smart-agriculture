package cn.agrinav.utils.mobile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * @author 11230
 */
public class PhoneNumberUtil {

    public boolean isPhoneNumber(String userPhone) {
        //这里是手机号的正则表达式
        String mobileRegEx = "^1[3-9][0-9]{9}$";
        //函数语法 匹配的正则表达式
        Pattern pattern = Pattern.compile(mobileRegEx);
        //进行匹配
        Matcher matcher = pattern.matcher(userPhone);
        return matcher.matches();
    }
    /**手机号加密*/
    public static String changePhoneNumber(String number){
        short length = 11;
        String changed;
        if(number.length()>length){
            number=number.substring(3);
        }
        changed = number.replaceAll("(\\d{3})(\\d{4})(\\d{4})", "$1****$3");
        return changed;
    }
}
