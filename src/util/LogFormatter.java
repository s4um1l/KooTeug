package util;


import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public class LogFormatter extends SimpleFormatter {

	@Override
	public String format(LogRecord record) {
		if(record.getLevel() == Level.INFO || record.getLevel() == Level.SEVERE ){
			return record.getMessage() + "\r\n";
		}else{
			return super.format(record);
		}
	}
}
