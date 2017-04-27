import com.opencsv.CSVReader;

import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

/**
 * Created by asanand on 4/27/17.
 * This only works for text files now. However, it can be easily modified to work for all kinds of files.
 */
public class CSVDetectorPy implements  CSVConstants{
    static int LOOK_AHEAD_SIZE = Integer.MAX_VALUE - 100; //determine it by size of file, do not hard code

    public static void main(String[] args) throws IOException {
        String workingDirectory = System.getProperty("user.dir");
        String pythonFilePath = workingDirectory+"/python/CSVDialectSniffer.py";
        FileInputStream fis = new FileInputStream("/Users/asanand/Downloads/Learning materials/datasets/csv/convertcsv1.csv");
        BufferedInputStream bis = new BufferedInputStream(fis);
        bis.mark(LOOK_AHEAD_SIZE);
        byte contents[] = new byte[READ_LIMIT];
        int bytesRead = bis.read(contents);
        String textContent = new String(contents,0,bytesRead);
        ProcessBuilder pb = new ProcessBuilder("python",pythonFilePath,textContent);
        Process p = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String strdelimiter = reader.readLine();
        //below is ,perhaps, a boring way to read metadata of CSV   file.
        HashMap<String,String> metadata = new HashMap<>();
        metadata.put(DELIMITER,strdelimiter);
        metadata.put(DOUBLE_QUOTE,reader.readLine());
        metadata.put(ESCAPE_CHARACTER,reader.readLine());
        // metadata.put(LINE_TERMINATOR,reader.readLine()); //Outputting line terminator has some buggy effect
        metadata.put(QUOTE_CHARACTER,reader.readLine());
        metadata.put(SKIP_INITIAL_SPACE,reader.readLine());
        String hasHeaderVlaue = reader.readLine();
        metadata.put(HAS_HEADER,hasHeaderVlaue);
        boolean hasHeader = hasHeaderVlaue !=null && hasHeaderVlaue.equalsIgnoreCase("true") ? true:false;
        String[] probableHeader= null;

        Character sniffedDelimiter = strdelimiter != null ?strdelimiter.charAt(0) : null;  // what will you do with null ? Halt program here
        bis.reset();

        final char[] delimiters = new char[]{sniffedDelimiter};
        try{
            boolean csvDetected = false;
            for (int i=0; i<delimiters.length;i++){
                char delimiter = delimiters[i];
                InputStreamReader isr = new InputStreamReader(bis,StandardCharsets.UTF_8);
                CSVReader csvr = new CSVReader(isr,delimiter);
                String[] nextLine;
                HashMap<Integer,Integer> fieldsCountMap = new HashMap<>();
                int line=0;
                int iteration =1;
                int prevAccuracy = 0;
                while(true){
                    while((nextLine=csvr.readNext()) != null &&  line < LINES_PER_ITERATION){
                        int numOfFields = nextLine.length;
                        if(line==0 && iteration ==1){ // This is to store header ,if present
                            probableHeader = new String[nextLine.length];
                            System.arraycopy(nextLine,0,probableHeader,0,nextLine.length);
                        }
                        if(fieldsCountMap.containsKey(numOfFields)) {
                            int count = fieldsCountMap.get(numOfFields);
                            count++;
                            fieldsCountMap.put(numOfFields, count);
                        }else{
                            fieldsCountMap.put(numOfFields,1);
                        }
                        line++;

                    }
                    //right now not handling a CSV with just one column
                    if(fieldsCountMap.keySet().size() == 1 && (Integer)fieldsCountMap.keySet().toArray()[0] ==1){
                        break;
                    }

                    if(fieldsCountMap.keySet().size() == 1 && iteration==1){
                        System.out.println("CSV detected ");
                        csvDetected = true;
                        break;
                    }
                    //below is code to detect corrupted csv
                    if(iteration <= MAX_ITERATIONS){
                        int tmpKey = 0;
                        Iterator<Map.Entry<Integer,Integer>> itr = fieldsCountMap.entrySet().iterator();
                        while(itr.hasNext()){
                            Map.Entry<Integer,Integer> entry = itr.next();
                            if(entry.getValue() < iteration*LEAST_ACCURACY_LEVEL){
                                itr.remove();
                            }else{
                                tmpKey=entry.getKey();
                            }
                        }

                        if(iteration ==5 && fieldsCountMap.keySet().size() ==1 && fieldsCountMap.get(tmpKey)/iteration >= prevAccuracy){
                            csvDetected = true;
                            System.out.println("Corrupted CSV detected with accuracy "+(fieldsCountMap.get(tmpKey)/iteration));
                            break;
                        }
                        if(fieldsCountMap.keySet().size()==0 || fieldsCountMap.get(tmpKey)/iteration < prevAccuracy){
                            break;
                        }

                        prevAccuracy = fieldsCountMap.get(tmpKey)/iteration;
                        line=0;
                        iteration++;
                    }

                }
                if(csvDetected){
                    break;
                }else{
                    bis.reset();
                }
            }
            if(!csvDetected){
                System.out.println("Not a CSV");
            }else{
                for(String key: metadata.keySet()){
                    System.out.println(key+"="+metadata.get(key));
                }
                if(hasHeader){
                    System.out.println("Header = "+Arrays.toString(probableHeader));

                }
            }

        }catch (Exception e){
            e.printStackTrace();

        }

    }
}
