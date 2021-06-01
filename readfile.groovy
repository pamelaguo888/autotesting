import jxl.*
import jxl.write.*
import net.sf.json.groovy.*
import groovy.json.JsonSlurper
import com.eviware.soapui.support.GroovyUtils
import groovy.json.JsonBuilder


try {

	def groovyUtils = new com.eviware.soapui.support.GroovyUtils(context)
	def projectPath = groovyUtils.projectPath
	
	File DB_connection = new File(projectPath + "\\DBConnectSetup.txt")
	def DB_connection_list = DB_connection.readLines()
	
	File source_and_target = new File(projectPath + "\\DBConfig.txt")
	def source_and_target_list = source_and_target.readLines()

	def dict_DB_connection = [:]
	def dict_source_target = [:]
	def source_DB
	def target_DB
	def source_DB_connect_str
	def target_DB_connect_str

	for (item in DB_connection_list) {
		//log.info(item)
		String[] str 
		str = item.split('=')
		//log.info(str[0])
		//log.info(str[1])
		dict_DB_connection[str[0]] = str[1]
	}
	//log.info(dict_DB_connection)

	for (item in source_and_target_list) {
		log.info(item)
		String[] str
		str = item.split('=')

		dict_source_target[str[0]] = str[1]
	}

	//setup DB connections for SoapUI
	//source DB
	source_DB = dict_source_target['Source']
	source_DB_connect_str = dict_DB_connection[source_DB]
	testRunner.testCase.testSuite.project.setPropertyValue("SOURCE_DB_CONNECT_STRING", source_DB_connect_str )
	
	//target DB
	target_DB = dict_source_target['Target']
	target_DB_connect_str = dict_DB_connection[target_DB]
	testRunner.testCase.testSuite.project.setPropertyValue("TARGET_DB_CONNECT_STRING", target_DB_connect_str )
	
	
} catch (Exception ex) {
	log.error(ex)
	throw(ex)
}
finally {
	log.info "DB Connections have been setup !!!"
}
