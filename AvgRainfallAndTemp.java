/**
 * AvgRainfallTemp reads a text file formatted with the following columns
 * StationID | Zipcode |  Lat |  Lon  |  Temp | Percip | Humid | Year | Month
 * and writes  out Zipcode | Year | AverageRainfall | AverageTemperature
 * 
 * Author: Alvin Thai, Daniel Holguin, Jarom Montgomerry, Nicholas Knaggs
 * Prof Silva ACO 432
 */

import java.io.IOException;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;



public class AvgRainfallAndTemp extends Configured implements Tool {

	public static class MyMapper extends Mapper<LongWritable, Text, Text, Text>
 	{	
		private static int STATION_ID	= 0;
		private static int ZIPCODE		= 1;
		private static int LAT			= 2;
		private static int LONG			= 3;
		private static int TEMP			= 4;
		private static int PERCIP		= 5;
		private static int HUMID		= 6;
		private static int YEAR			= 7;
		private static int MONTH		= 8;
		
 		public void map(LongWritable inputKey, Text inputValue, Context context) throws IOException, InterruptedException
 		{
 			//StationID | Zipcode |  Lat      |  Lon       |  Temp   | Percip | Humid | Year | Month 
 			String[] element = new String[9];
 			StringTokenizer st = new StringTokenizer(inputValue.toString());
 			
 			for(int i = 0; i < element.length; i++)
 			{
	 			if(st.hasMoreTokens())
	 			{
	 				element[i] = st.nextToken();
	 				if(i == 0 && element[i].equals("StationID"))
	 					return;
	 			}
				else 
					return;
 			} //end for
 			Integer year = Integer.parseInt(element[YEAR]);
 			if (year >= 1950){
 				context.write(new Text(element[ZIPCODE] + " " + element[YEAR]), new Text(element[PERCIP] + " " + element[TEMP]));
 			}
 			
 			
 		}//end map
 		
	}//end MyMapper
 		
	public static class MyReducer extends Reducer<Text, Text, Text, Text>
	{
		
		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException
		{
			double sumTemp = 0.0d;
			double sumPercip = 0.0d;
			long counter = 0;
			Iterator<Text> valItr = values.iterator();
			
			while(valItr.hasNext())
			{
				String [] sValues = valItr.next().toString().split(" ");
				double temp = new Double(Double.parseDouble(sValues[0]));
				double percip = new Double(Double.parseDouble(sValues[1]));
				sumTemp += temp;
				sumPercip += percip;
				counter++;
			}
			context.write(key, new Text(Math.round((sumTemp / counter)*100.0)/100.0 + " " + Math.round((sumPercip / counter)*100.0)/100.0));
		}//end reduce
		
	}//end MyReducer
 	
	public static void main(String[] args) throws Exception
	{
		int res = ToolRunner.run(new Configuration(), new AvgRainfallAndTemp(), args);
		System.exit(res);
	}
	
	public int run(String[] args) throws Exception 
	{
		if(args.length != 3)
		{
			System.out.println("Usage: bin/hadoop jar InstructionalSolutions.jar AverageTempByZipCode <input directory> <ouput directory> <number of reduces>");
			System.out.println("args length incorrect, length: " + args.length);
			return -1;
		}
		int numReduces;
		
		Path inputPath = new Path(args[0]);
		Path outputPath = new Path(args[1]);
		
		try
		{
			numReduces 	= new Integer(args[2]);
			System.out.println("number reducers set to: " + numReduces);
		}
		catch(NumberFormatException e)
		{
			System.out.println("Usage: bin/hadoop jar InstructionalSolutions.jar AverageTempByZipCode <input directory> <ouput directory> <number of reduces>");
			System.out.println("Error: number of reduces not a type integer");
			return -1;
		}
		
		Configuration conf = new Configuration();
		
		FileSystem fs = FileSystem.get(conf);
		
		if(!fs.exists(inputPath))
		{
			System.out.println("Usage: bin/hadoop jar InstructionalSolutions.jar AverageTempByZipCode <input directory> <ouput directory> <number of reduces>");
			System.out.println("Error: Input Directory Does Not Exist");
			System.out.println("Invalid input Path: " + inputPath.toString());
			return -1;
		}
		
		if(fs.exists(outputPath))
		{
			System.out.println("Usage: bin/hadoop jar InstructionalSolutions.jar AverageTempByZipCode <input directory> <ouput directory> <number of reduces>");
			System.out.println("Error: Output Directory Already Exists");
			System.out.println("Please delete or specifiy different output directory");
			return -1;
		}
		Job job = new Job(conf, "MapReduceShell Test");
		
		job.setNumReduceTasks(numReduces);
		job.setJarByClass(AvgRainfallAndTemp.class);
		
		//sets mapper class
		job.setMapperClass(MyMapper.class);
		
		//sets map output key/value types
	    job.setMapOutputKeyClass(Text.class);
	    job.setMapOutputValueClass(Text.class);
	    
		//Set Reducer class
	    job.setReducerClass(MyReducer.class);
	    
	    // specify output types
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);	
		
		//sets Input format
	    job.setInputFormatClass(TextInputFormat.class);
	    
	    // specify input and output DIRECTORIES (not files)
		FileInputFormat.addInputPath(job, inputPath);
		FileOutputFormat.setOutputPath(job, outputPath);
		
		
		job.waitForCompletion(true);
		return 0;
	}
}
