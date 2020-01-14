package com.chocho.bouncylegs.sensorProc;

import android.os.Environment;
import android.util.Log;

import com.chocho.bouncylegs.Constants;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;


public class DataInstanceList extends ArrayList<DataInstance> {

	final static String TAG = "DataInstanceList";

	private long timeId; // Unixtime for window time id

	public DataInstanceList(){
		;
	}

	public DataInstanceList(long timeId){
		this.timeId = timeId;
	}

	public long getTimeId(){
		return timeId;
	}

	public void saveToCsvFile(String fileName){

		try {
			String pathBase = Environment.getExternalStorageDirectory() + "/" + Constants.WORKING_DIR_NAME;
			File pathFinal = new File(pathBase);
			pathFinal.mkdirs();

			File outputFile = new File(pathFinal, fileName);
			FileWriter fw = new FileWriter(outputFile, false);
			for(DataInstance di : this){
				
				long unixtime = di.getUnixtime();
				float[] values = di.getValues();
				String label = di.getLabel();
				
				StringBuilder sbValues = new StringBuilder();
				sbValues.append(unixtime);
				for(float v : values){
					sbValues.append(v+",");
				}
				sbValues.append(label);
				sbValues.append("\n");
				
				fw.write(sbValues.toString());
			}
			fw.flush();
			fw.close();


		} catch (Exception e){
			Log.e(TAG, e.getMessage());
		}
	}

	public String toString(){
		StringBuilder sb = new StringBuilder();

		sb.append(timeId + "(" + this.size() + ") ");
		sb.append("{");
		for(int i=0; i<this.size(); i++){
			DataInstance di = this.get(i);
			sb.append(di.getUnixtime());
			if(i != this.size() -1)
				sb.append(",");
		}
		sb.append("}");

		return sb.toString();

	}

}

