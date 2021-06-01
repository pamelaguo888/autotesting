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

        def DataList = []
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
            DataList.add(itemMap)         
        }

        return DataList
		
	}
	catch (Exception ex){
		log.error(ex)
		throw(ex)
	}
}


def findTargetItems(List sourceItems, String identifierName, String identifierValue) {  
    //log.info "$identifierName : $identifierValue"
    //def size = sourceItems.size()
    //log.info "source items size = $size"
    if(sourceItems.size() > 1){
        //sourceItems = sourceItems.findAll{it["ACCT_CD"]== "CRDOPTY"}
        sourceItems = sourceItems.findAll { it[identifierName] == identifierValue } 
    }

    return sourceItems
}


try {
		
	log.info "$testRunner.testCase.name verification start..."
	List dbXmlSource = parsed_response("Source DB Fetch")
    List dbXmlTarget = parsed_response("Target DB Fetch")

	List items_in_base_but_not_target = []

    log.info "dbXmlSource size = " + dbXmlSource.size()
	log.info "dbXmlTarget size = " + dbXmlTarget.size()
    def base_record_size = dbXmlSource.size()
	def target_record_size = dbXmlTarget.size()

    String[] identifiers = testRunner.testCase.getPropertyValue("Identifier").split(',')
    log.info "Using Identifiers : $identifiers"
	for (item in dbXmlSource) {
        //item.each{entry -> log.info "Source - $entry.key : $entry.value" }
        def targetItems = dbXmlTarget.clone()
        for (String identifier in identifiers){
                
            targetItems = findTargetItems(targetItems, identifier, item[identifier])
            //def size = targetItems.size()
            //log.info "Found $size target item(s) for $identifier"
        }
        //log.info targetItems
        if(!item.equals(targetItems.find{true})){  
                items_in_base_but_not_target.add(item)
        }
		
	}


	log.info "items_in_base_but_not_target size = " + items_in_base_but_not_target.size()

	def groovyUtils = new com.eviware.soapui.support.GroovyUtils(context)
	def projectPath = groovyUtils.projectPath
	if(items_in_base_but_not_target.size() > 0){
        def fileName = testRunner.testCase.name
	    File outputFile = new File(projectPath + context.expand('\\${#Project#OUTPUT_FOLDER}\\') + fileName + ".csv")
    
        log.info "write results into file: $outputFile "
        items_in_base_but_not_target.eachWithIndex { item, index ->
            //item.each{entry -> log.info "Key:$entry.key, Value:$entry.value" }
            // Add File Header Row
            if(index == 0){
                 def header = ""
                 item.each{entry -> header += entry.key + ","}
                 log.info "Header: $header"
                 outputFile.text = header + "\n"
            }
            def dataValue = ""       
            item.each{entry -> dataValue += entry.value + ","}
	    	outputFile.append(dataValue + "\n")
	    }
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
