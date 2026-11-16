package cn.agrinav.front.util;

import java.util.List;

/**
 * @author 18506
 */
public class TransformUtils {
    public static String transformRegion(List<String> list){
        StringBuilder builder = new StringBuilder();
        for (String s : list){
            builder.append(s).append(" ");
        }
        return builder.toString();
    }
}
