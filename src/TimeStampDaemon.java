import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class TimeStampDaemon {
    private static File file = new File("src/logs.txt");

    private static ArrayList<String> tempLogs = new ArrayList<>();

    public static void startDaemon() {
        System.setOut(new PrintStream(System.out){
            private final SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");

            @Override
            public void println(String message) {
                super.println(message);
                String newLog = formatter.format(new Date())+":"+message;
                tempLogs.add(newLog);

                //allow forced flushing on guard value so that we can shut down non-daemon threads safely without losing logging data by forcing a flush first
                if(message.equals("FLUSH_LOGS_TO_FILE")){
                    flushLogs();
                }
            }
        });

        Thread timePrinter = new Thread(() -> {
            while (true) {
                try{
                    flushLogs();
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                }
            }
        });

        timePrinter.setDaemon(true);
        timePrinter.start();
    }

    private static void flushLogs(){
        try {
            FileWriter writer = new FileWriter(file);
            for(String eventLog : tempLogs){
                writer.write(eventLog + "\n");
            }
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}