import com.opencsv.CSVReader;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.*;
import java.nio.charset.StandardCharsets;

/**
 * Created by asanand on 4/21/17.
 */
public class CSVDetector implements  CSVConstants{
    static int LOOK_AHEAD_SIZE = Integer.MAX_VALUE - 100;

    public static void main(String[] args) throws FileNotFoundException {
        FileInputStream fis = new FileInputStream("/Users/asanand/Downloads/Learning materials/datasets/csv/convertcsv1.csv");
        BufferedInputStream bis = new BufferedInputStream(fis);
        bis.mark(LOOK_AHEAD_SIZE);
        final char[] delimiters = new char[]{';',','};
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
                        System.out.println("csv detected with delimiter "+delimiter);
                        csvDetected = true;
                        break;
                    }
                    //below is code to detect corrupted csv
                    if(iteration <= MAX_ITERATIONS){
                        int tmpKey = 0;
                        Iterator<Map.Entry<Integer,Integer>> itr = fieldsCountMap.entrySet().iterator();
                        while(itr.hasNext()){
                            Map.Entry<Integer,Integer> entry = itr.next();
                            if(entry.getValue() < iteration*90){
                                itr.remove();
                            }else{
                                tmpKey=entry.getKey();
                            }
                        }

                        if(iteration ==5 && fieldsCountMap.keySet().size() ==1 && fieldsCountMap.get(tmpKey)/iteration >= prevAccuracy){
                            csvDetected = true;
                            System.out.println("Corrupted CSV detected with delimiter "+delimiter+" and accuracy "+(fieldsCountMap.get(tmpKey)/iteration));
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
            }

        }catch (Exception e){
            e.printStackTrace();

        }

    }
}
