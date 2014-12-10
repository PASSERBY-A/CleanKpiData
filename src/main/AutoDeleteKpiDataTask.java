package main;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;



@Service
public class AutoDeleteKpiDataTask {

	final private Log log = LogFactory.getLog(AutoDeleteKpiDataTask.class);
	
	private int dayConut=-30;
	
	private JdbcTemplate jdbcTemplate;
	
	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}
	
	public void setDayConut(int dayConut) {
		this.dayConut = dayConut;
	}
	
	public int getDayConut() {
		return dayConut;
	}

	public void execute()
	{
		
		log.info("running ..............");
		
		log.info("dayCount  .............."+dayConut);
		
		
		final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		
		final Calendar clearDate =  Calendar.getInstance();
		
		clearDate.setTime(new Date(System.currentTimeMillis()));
		
		clearDate.add(Calendar.DAY_OF_YEAR, dayConut);
		
		log.debug("clear date is :"+sdf.format(new Date(clearDate.getTimeInMillis())));
		
		
		String partitionsSql = "select TABLE_NAME,PARTITION_NAME,TABLESPACE_NAME from user_tab_partitions where table_name='TF_AVMON_KPI_VALUE'";
		
		
		jdbcTemplate.query(partitionsSql, new RowMapper() {

			@Override
			public Object mapRow(ResultSet rs, int index) throws SQLException {
				
				final StringBuffer dataSql = new StringBuffer("select to_char(kpi_time, 'yyyy-mm-dd') as kpi_time from TF_AVMON_KPI_VALUE partition (");

				
				final String pname = rs.getString("PARTITION_NAME");
				if(null!=pname && !"".equals(pname.trim()))
				{
					dataSql.append(pname);
					dataSql.append(") where rownum <=1 ");
					
					jdbcTemplate.query(dataSql.toString(), new RowMapper(){

						@Override
						public Object mapRow(ResultSet rs, int index)
								throws SQLException {
							
							String kpiTime = rs.getString("kpi_time");
							
							
							try {
								long  ktime = sdf.parse(kpiTime).getTime();
								
								if(ktime<clearDate.getTimeInMillis())
								{
									 String sql = "alter table TF_AVMON_KPI_VALUE drop partition " + pname + " update global indexes";
									 
									 log.info("clear partition is :"+pname);
									 
									 jdbcTemplate.execute(sql);
									 
									 log.info("delete partition "+pname+"successful! ");
								}
								else{
									
									
									log.info("There are don't have any rows be delete . ");
									
								}
							} catch (ParseException e) {
							 
								log.error(e.getMessage());
							}
							return null;
						}});
				}
				return null;
			}
		});
		
		
	}
	
	
	
	public static void main(String[] args) {
		
		/**/
		
		ApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");
		
		//AutoDeleteKpiDataTask auto = 	context.getBean(AutoDeleteKpiDataTask.class);
		//auto.run();
		
		/*SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		
		Calendar c =  Calendar.getInstance();
		
		c.setTime(new Date(System.currentTimeMillis()));
		
		c.add(Calendar.DAY_OF_YEAR, 1);
		
		System.out.println(sdf.format(new Date(c.getTimeInMillis())));
		
		
		System.out.println(System.currentTimeMillis()<c.getTimeInMillis());*/
	}
	
	
	
	
	
	
	
	
	
	
}
