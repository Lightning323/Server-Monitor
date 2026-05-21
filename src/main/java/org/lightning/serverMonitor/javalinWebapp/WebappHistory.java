package org.lightning.serverMonitor.javalinWebapp;

import org.lightning.serverMonitor.logging.HistoryRecord;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.lightning.serverMonitor.logging.HistoryRecord.RECORD_HEADER;

class WebappHistory {
    public final ArrayList<HistoryRecord> recordData = new ArrayList<>();

    public static final String TEMP_HISTORY_DIR = "history";
    File csvFile;

    public WebappHistory() {
        // Generate a timestamp for the filename
        //The start date of the history
        String timestamp = new SimpleDateFormat("yyyy-MM-dd (HH-mm-ss)").format(new Date());
        csvFile = new File(TEMP_HISTORY_DIR, timestamp + ".csv");
        csvFile.getParentFile().mkdirs();

        try (FileWriter writer = new FileWriter(csvFile, false)) {
            writer.write(RECORD_HEADER);// Write CSV headers
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public HistoryRecord addRecord(double temp, double load) {
        HistoryRecord record = new HistoryRecord(System.currentTimeMillis(), temp, load);
        this.recordData.add(record);

        try (FileWriter writer = new FileWriter(csvFile, true)) {
            writer.write(record.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return record;
    }

    public int size() {
        return recordData.size();
    }


    public String getRecordsAsString(String prefix) {
        String history = "";
        for (int i = 0; i < recordData.size(); i++) {
            history += recordData.get(i).toString(prefix);
        }
        return history;
    }

    public String getRecordsAsString(String prefix, int start, int end, int step) {
        String records = "";
        for (int i = start; i < Math.min(end, this.recordData.size()); i++) {
            if (i % step != 0) continue;
            records += recordData.get(i).toString(prefix);
        }
        return records;
    }

    public String getRecordsAsString(String prefix, String externalFile) {
        StringBuilder history = new StringBuilder();
        try {
            List<String> strings = Files.readAllLines(Paths.get(TEMP_HISTORY_DIR, externalFile));
            //Return all except the first line
            for (int i = 1; i < strings.size(); i++) {
                history.append(prefix).append(strings.get(i)).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return history.toString();
    }

}
