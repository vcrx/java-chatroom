package Utils;

import java.util.Date;
import java.text.SimpleDateFormat;

public class TimeUtils {
	public static String parseTimeStamp(Long timeStamp) {
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(Long.parseLong(String.valueOf(timeStamp))));
	}
}
