class ExecutionInfo {

	static belongsTo = Job

	Job job
	def cluster
	def static fieldNames = [
		 'getServerInfoREAL', 
		 'getFunctionInfoREAL', 
		 'gramInvokeREAL', 
		 'getServerInfoCPU', 
		 'getFunctionInfoCPU', 
		 'gramInvokeCPU', 
		 'transferArgumentREAL', 
		 'transferFile1REAL', 
		 'calculationREAL', 
		 'transferResultREAL', 
		 'transferFile2REAL', 
		 'transferArgumentCPU', 
		 'transferFile1CPU', 
		 'calculationCPU', 
		 'transferResultCPU', 
		 'transferFile2CPU', 
		 'callbackcount', 
		 'transferArgumentREALS1', 
		 'transferFile1REALS2', 
		 'calculationREALS3', 
		 'transferResultREALS4', 
		 'transferFile2REALS5', 
		 'transferArgumentCPUS6', 
		 'transferFile1CPUS7', 
		 'calculationCPUS8', 
		 'transferResultCPUS9', 
		 'transferFile2CPUS10', 
		 'callbackcountS11', 
		 'beforecompressionlength', 
		 'aftercompressionlength', 
		 'compressionTimeReal', 
		 'compressionTimeCPU', 
		 'beforedecompressionlength', 
		 'afterdecompressionlength', 
		 'decompressionTimeReal', 
		 'decompressionTimeCPU', 
		 'beforecompressionlengthS12', 
		 'aftercompressionlengthS13', 
		 'compressionTimeRealS14', 
		 'compressionTimeCPUS15', 
		 'beforedecompressionlengthS16', 
		 'afterdecompressionlengthS17', 
		 'decompressionTimeRealS18', 
		 'decompressionTimeCPUS19', 
		 'beforecompressionlengthS20', 
		 'aftercompressionlengthS21', 
		 'compressionTimeRealS22', 
		 'compressionTimeCPUS23', 
		 'beforedecompressionlengthS24', 
		 'afterdecompressionlengthS25', 
		 'decompressionTimeRealS26', 
		 'decompressionTimeCPUS27', 
		 'beforecompressionlengthS28', 
		 'aftercompressionlengthS29', 
		 'compressionTimeRealS30', 
		 'compressionTimeCPUS31', 
		 'beforedecompressionlengthS32', 
		 'afterdecompressionlengthS33', 
		 'decompressionTimeRealS34', 
		 'decompressionTimeCPUS35' 
	]
	
	double getServerInfoREAL = 0.0  
	double getFunctionInfoREAL = 0.0  
	double gramInvokeREAL = 0.0  
	double getServerInfoCPU = 0.0  
	double getFunctionInfoCPU = 0.0  
	double gramInvokeCPU = 0.0  
	double transferArgumentREAL = 0.0  
	double transferFile1REAL = 0.0  
	double calculationREAL = 0.0  
	double transferResultREAL = 0.0  
	double transferFile2REAL = 0.0  
	double transferArgumentCPU = 0.0  
	double transferFile1CPU = 0.0  
	double calculationCPU = 0.0  
	double transferResultCPU = 0.0  
	double transferFile2CPU = 0.0  
	double callbackcount = 0.0  
	double transferArgumentREALS1 = 0.0  
	double transferFile1REALS2 = 0.0  
	double calculationREALS3 = 0.0  
	double transferResultREALS4 = 0.0  
	double transferFile2REALS5 = 0.0  
	double transferArgumentCPUS6 = 0.0  
	double transferFile1CPUS7 = 0.0  
	double calculationCPUS8 = 0.0  
	double transferResultCPUS9 = 0.0  
	double transferFile2CPUS10 = 0.0  
	double callbackcountS11 = 0.0  
	double beforecompressionlength = 0.0  
	double aftercompressionlength = 0.0  
	double compressionTimeReal = 0.0  
	double compressionTimeCPU = 0.0  
	double beforedecompressionlength = 0.0  
	double afterdecompressionlength = 0.0  
	double decompressionTimeReal = 0.0  
	double decompressionTimeCPU = 0.0  
	double beforecompressionlengthS12 = 0.0  
	double aftercompressionlengthS13 = 0.0  
	double compressionTimeRealS14 = 0.0  
	double compressionTimeCPUS15 = 0.0  
	double beforedecompressionlengthS16 = 0.0  
	double afterdecompressionlengthS17 = 0.0  
	double decompressionTimeRealS18 = 0.0  
	double decompressionTimeCPUS19 = 0.0  
	double beforecompressionlengthS20 = 0.0  
	double aftercompressionlengthS21 = 0.0  
	double compressionTimeRealS22 = 0.0  
	double compressionTimeCPUS23 = 0.0  
	double beforedecompressionlengthS24 = 0.0  
	double afterdecompressionlengthS25 = 0.0  
	double decompressionTimeRealS26 = 0.0  
	double decompressionTimeCPUS27 = 0.0  
	double beforecompressionlengthS28 = 0.0  
	double aftercompressionlengthS29 = 0.0  
	double compressionTimeRealS30 = 0.0  
	double compressionTimeCPUS31 = 0.0  
	double beforedecompressionlengthS32 = 0.0  
	double afterdecompressionlengthS33 = 0.0  
	double decompressionTimeRealS34 = 0.0  
	double decompressionTimeCPUS35 = 0.0  

	static constraints = { 
		job(nullable:true)
		cluster(nullable:true)
		gramInvokeREAL(nullable:true)  
		transferArgumentREAL(nullable:true)  
		calculationREAL(nullable:true)  
		transferResultREAL(nullable:true)  
	}

	def parseExecutionInfo( executionInfo ) {
		try {
			if( executionInfo == "null" ) { return }
			def pairs = executionInfo.toString().split(/\n/)
			def valueList = []
			pairs?.each { entry ->
				if( entry =~ /^[a-zA-Z]/ ) {
					def tokens = entry.split(/:/)
					def value = tokens[1]
					valueList << value
				}
			}
			if( valueList ) {
				def pos = 0
				fieldNames.each { prop ->
					def value = (valueList[pos])? valueList[pos].trim() : "0.0"
					this."$prop" = Double.parseDouble( value )
					pos++
				}
			}
		}
		catch ( Exception e ) {
			log.error("ExecutionInfo *SAVEINFO* [${job}] ERROR $e");
		}
	}

	String toString() { job?.name }
}
