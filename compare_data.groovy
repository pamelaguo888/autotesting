import jxl.*
import jxl.write.*
import net.sf.json.groovy.*
import groovy.json.JsonSlurper
import com.eviware.soapui.support.GroovyUtils
import groovy.json.JsonBuilder

def parsed_response(String jdbcName) {
	try {
		def groovyUtils = new com.eviware.soapui.support.GroovyUtils(context)
        def DBholder = groovyUtils.getXmlHolder(jdbcName + "#ResponseAsXml")

        def DataList = [:]
        def i = 0;
        for( node in DBholder.getDomNodes( "//Row" )){
            //log.info "Item : [$node]"
            def itemMap = [:]
            node.getChildNodes().each { child ->
                
                if(child.getNodeName() != "#text"){    
                    def curr_nodeName = child.getNodeName()                   
                    //log.info "Node_Name: [$curr_nodeName]"
                    def curr_nodeValue = com.eviware.soapui.support.xml.XmlUtils.getNodeValue(child)       
                    //log.info "Node_Value: [$curr_nodeValue]"
                    itemMap.put(curr_nodeName, curr_nodeValue)
                }
                      
            }
            DataList.put(i++, itemMap)         
        }

        return DataList
		
	}
	catch (Exception ex){
		log.error(ex)
		throw(ex)
	}
}

def remove_item_from_list(Map item, List list) {
	for (int i=0; i<list.size(); i++) {
		if (item.equals(list[i])) {
			list.remove(i)
			break
		}
	}
}

try {
		
	log.info "$testRunner.testCase.name verification start..."
	dbXmlSource = parsed_response("Source DB Fetch")
	dbXmlTarget = parsed_response("Target DB Fetch")

	items_in_base_but_not_target = [:]

    log.info "dbXmlSource size = " + dbXmlSource.size()
	log.info "dbXmlTarget size = " + dbXmlTarget.size()
    def base_record_size = dbXmlSource.size()
	def target_record_size = dbXmlTarget.size()

    def j = 0
	for (item in dbXmlSource) {
        //item.each{entry -> log.info "Source - $entry.key : $entry.value" }
        // Get a test case property
        def identifier = testRunner.testCase.getPropertyValue("Identifier")
        def targetItem = dbXmlTarget.find { it.value[identifier] == item.value[identifier] } // find a single entry
        //targetItem.each{entry -> log.info "Target - $entry.key : $entry.value" }

        if(!item.equals(targetItem)){
            //log.info "Key:$item.key, Value:$item.value"
            items_in_base_but_not_target.put(j++, item.value)
        }
		//else {
			//remove_item_from_list(item, dbXmlSource)
		//}
	}


	log.info "items_in_base_but_not_target size = " + items_in_base_but_not_target.size()

	log.info "write results into file......"

	def groovyUtils = new com.eviware.soapui.support.GroovyUtils(context)
	def projectPath = groovyUtils.projectPath
	
    //log.info testRunner.testCase.testSuite.project.name
    //log.info testRunner.testCase.name
    def fileName = testRunner.testCase.name
	File outputFile = new File(projectPath + context.expand('\\${#Project#OUTPUT_FOLDER}\\') + fileName + ".csv")
	

    for(item in items_in_base_but_not_target) {
        //log.info "Key:$item.key, Value:$item.value"
        if(item.key == 0){
            def header = ""
            for(data in item.value){
                header += data.key + ","
            }
            outputFile.text = header + "\n"
        }
        def datavalue = ""       
        for(data in item.value){
            //log.info "Data -  $data.key, $data.value"
            datavalue += data.value + ","
        }
		outputFile.append(datavalue + "\n")
	}
	
    File summary_file = new File(projectPath + context.expand('\\${#Project#OUTPUT_FOLDER}') + "\\SUMMARY_Main.csv")
	def summary_line = "$testRunner.testCase.name" + "," + base_record_size + "," + target_record_size + "," + items_in_base_but_not_target.size()
	summary_file.append(summary_line+"\n")
	
} catch (Exception ex) {
	log.error(ex)
	throw(ex)
}
finally {
	log.info "$testRunner.testCase.name verification finished!!!"
}
