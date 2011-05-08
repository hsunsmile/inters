
import org.springframework.web.servlet.ModelAndView
import java.io.StringWriter
import java.text.SimpleDateFormat
import groovy.xml.MarkupBuilder

class JobController {

	def scaffold = Job    
	def schedulerCoreService
	def advisorInfoService
	def jobInfoService
	def executionAdviceService

	def defaultAction = "monitor"

	def doNothing = {
		render('')
	}

	def pause = {
		Thread.start { schedulerCoreService.pause(); }
		render( view:"monitor" );
	}

	def resume = {
		Thread.start { schedulerCoreService.resume(); }
		render( view:"monitor" );
	}

	def reschedule = {
		Thread.start { schedulerCoreService.reschedule(); }
		render( view:"monitor" );
	}

	def advise = {
		render( view:"advise" )
	}

	def giveAdvise = {
		render( text: "<g:link action=\"monitor\">Show Jobs</g:link> ${new Date()} " )
	}

	def monitor = {
		render( view:"monitor" )
	}

	def showGraph = {
		def type = (params.type)? params.type : "clusters"
		int limit = (params.limit)? params.limit.toInteger() : 20
		def writer = new StringWriter()
		def values
		switch( type ) {
			case "all":
					if( !Job.count() ) { render " No Job Submited ! "; break }
					def iter = (long)(Job.count()/limit)
					iter += (Job.count()%limit)? 1:0
					iter.downto(1) {
						values = [ height:250, width:480, bgcolor:"#ccbbff",
								   link:"/scheduler/job/joblist?start%3D${(it-1)*limit}%26limit%3D${limit}" ]
						buildGraph( writer,values )
					}
					break
			case "functions":
					RemoteFunction.list().each { func ->
						def jobs = Job.list().findAll{ it.function.name == func.name }
						if( !jobs ) { return }
						def iter = (long)( jobs.size()/limit )
						iter += (jobs.size()%limit)? 1:0
						iter.downto(1) {
							values = [ height:250, width:480, bgcolor:"#ccaaff", 
								link:"/scheduler/job/joblistByFunc?funcName=${func.name}" + 
								"%26start%3D${(it-1)*limit}%26limit%3D${limit}" ]
							buildGraph( writer,values )
						}
					}
					break
			case "clusters":
					Cluster.list().each { cluster ->
						def jobs = Job.list().findAll{ it.cluster?.name == cluster.name }
						if( !jobs ) { return }
						def iter = (long)( jobs.size()/limit )
						iter += (jobs.size()%limit)? 1:0
						iter.downto(1) {
							values = [ height:250, width:480, bgcolor:"#ffffff", 
								link:"/scheduler/job/joblistByCluster?funcName=${cluster.name}" + 
								"%26start%3D${(it-1)*limit}%26limit%3D${limit}" ]
							buildGraph( writer,values )
						}
					}
					break
		}
		render( writer.toString() )
	}

	def buildGraph( StringWriter writer, values ) {
		def html = new MarkupBuilder( writer )
		def height = values.height, width = values.width, update_link = values.link, bgcolor = values.bgcolor
		html.span( align:"center" ) {
				// html.testargs(type)
				html.OBJECT( classid:"clsid:D27CDB6E-AE6D-11cf-96B8-444553540000",
								codebase:"http://download.macromedia.com/pub/shockwave/cabs/flash/swflash.cab#version:6,0,0,0",
								WIDTH:"${width}", HEIGHT:"${height}", id:"charts", ALIGN:"" )
				{
					html.PARAM( NAME:"movie", VALUE:
							"/scheduler/charts.swf?library_path=/scheduler/charts_library&xml_source=${update_link}")
					html.PARAM( NAME:"quality", VALUE:"high" )
					html.PARAM( NAME:"bgcolor", VALUE:bgcolor )
					html.EMBED( src:
							"/scheduler/charts.swf?library_path=/scheduler/charts_library&xml_source=${update_link}",
							quality:"high", bgcolor:bgcolor, WIDTH:width, HEIGHT:height, NAME:"charts", ALIGN:"",
							swLiveConnect:"true", TYPE:"application/x-shockwave-flash", 
							PLUGINSPAGE:"http://www.macromedia.com/go/getflashplayer", "" )
			}
		}
	}

	def joblistByCluster = {
		int start = params.start.toInteger(), limit = params.limit.toInteger()
		def clusterName = params.funcName
		def writer = new StringWriter()
		def jobs = Job.list()
		if( jobs ) {
			Closure filter = { true }
			def tjobs = jobs.findAll { it.cluster?.name == clusterName }
			if( tjobs.size() < start ) start = tjobs.size();
			def njobs= tjobs.subList( start, (start+limit as BigInteger ).min(tjobs.size() as BigInteger) )
			def valueMap = [ chart_rect: [ x:'45', y:'25', width:'400', height:'200' ], 
				text: [ funcName:"${clusterName}", size:15, x:150, y:4, color:'000000', alpha:90 ], 
				live_update:[ link: "/scheduler/job/joblistByCluster?funcName=${clusterName}&start=${start}&limit=${limit}" ],
				series_color:['668866','ff8800','0000ff'] ]
			changeJobToXML( njobs, filter, writer, valueMap )
			render( contentType:"text/xml", text:writer.toString() )
		} else {
			render( contentType:"text/xml", text:"<chart></chart>" )
		}
	}

	def joblistByFunc = {
		int start = params.start.toInteger(), limit = params.limit.toInteger()
		def funcName = params.funcName
		def writer = new StringWriter()
		def jobs = Job.list()
		if( jobs ) {
			Closure filter = { true } //it.status != "canceled" }
			def tjobs = jobs.findAll { it.function.name == funcName }
			def njobs= tjobs.subList( start, (start+limit as BigInteger ).min(tjobs.size() as BigInteger) )
			def valueMap = [ chart_rect: [ x:'45', y:'25', width:'400', height:'200' ], 
				text: [ funcName:"${funcName}", size:25, x:150, y:70, color:'ff0000', alpha:30 ], 
				live_update:[ link: "/scheduler/job/joblistByFunc?funcName=${funcName}&start=${start}&limit=${limit}" ],
				series_color:['668866','ff8800','668899'] ]
								changeJobToXML( njobs, filter, writer, valueMap )
			render( contentType:"text/xml", text:writer.toString() )
		} else {
			render( contentType:"text/xml", text:"<chart></chart>" )
		}
	}

	def joblist = {
		def writer = new StringWriter()
		int start = params.start.toInteger(), limit = params.limit.toInteger()
		def end_point = (start+limit as BigInteger).min(Job.count() as BigInteger)
		def jobs;
		if( end_point > start ) jobs = Job.list().subList( start, end_point )
		if( jobs ) {
			def valueMap = [ chart_rect: [ x:'45', y:'25', width:'400', height:'200' ],
			text: [ funcName:"Job Report", size:25, x:150, y:70, color:'ffffff', alpha:30 ], 
			live_update:[ link: "/scheduler/job/joblist?start=${start}&limit=${limit}" ], 
			series_color:['88ff00','ff8800','88ffee'] ]
			Closure filter = { obj -> true } //obj.status != "canceled" }
			changeJobToXML( jobs, filter, writer, valueMap )
			render( contentType:"text/xml", text:writer.toString() )
		} else {
			render( contentType:"text/xml", text:"<chart></chart>" )
		}
	}

	def changeCluster = {
		def joblist = []
		println " [DBG] params: $params "
		def destClusterId = params.cluster.id
		params.ids.split(',').each { jobid ->
			def jobId = jobid.toInteger();
			executionAdviceService.makeJobMigrationAdvise( jobId, destClusterId );
			jobInfoService.changeJobCluster( jobId, destClusterId )
		}
		println " [DBG] will render result for changeCluster "
		render( " job in $params.ids changed to $destCluster " )
	}

	def changeCluster_old = {
		def joblist = []
		println " [DBG] params: $params "
		def destCluster = Cluster.get(params.cluster.id) 
		Cluster.list().each { cluster ->
			joblist += advisorInfoService.fetchRunningJobListByCluster( cluster )
		}
		params.ids.split(',').each { jobid ->
			def ajob = joblist.find { it.jid == jobid.toInteger() && it.cluster.name != destCluster.name }
			println " [CHG] $ajob "
			if(ajob) jobInfoService.changeJobCluster( ajob.id, destCluster.id )
			println " [RSCHE] $ajob "
			if(ajob) schedulerCoreService.addJob( ajob ) 
			// advisorInfoService.renewJobMap( ajob )
		}
		println " [DBG] will render result for changeCluster "
		render( " job in $params.ids changed to $destCluster " )
	}

	def changeJobToXML( jobs, Closure filter, StringWriter writer, valueMap = null, String chartType = 'stackedcolumn' ) {
		switch( chartType ) {
			case "Scatter":
					makeScatterChart( jobs, writer )
					break
			default:
					makeStackColumnChart( jobs, filter, writer, valueMap )
		}
	}

	def makeScatterChart( jobs, StringWriter writer ) {
		if( !jobs ) return
		def xml = new MarkupBuilder(writer)
		xml.chart {
			def maxNum = -1; 
			Cluster.list().each { def cmp = it.calcTotalAvailableNodesNum(); maxNum = ( maxNum < cmp )? cmp : maxNum }
			xml.axis_category( "size":'10', color:'ffffff', alpha:'50', font:'arial', 
									bold:'true', skip:'0', orientation:'horizontal')
			xml.axis_ticks( value_ticks:'true', category_ticks:'true', major_thickness:'2', minor_thickness:'1', 
									minor_count:'1', major_color:'000000', minor_color:'222222', position:'outside' )
			xml.axis_value( min:'0', max:maxNum+1, font:'arial', bold:'true', "size":'10', color:'ffffff', 
						alpha:'50', steps:maxNum, prefix:'', suffix:'', decimals:'0', 
						separator:'', show_min:'true' )
			xml.chart_border( color:'000000', top_thickness:'2', 
							bottom_thickness:'2', left_thickness:'2', right_thickness:'2' )

			   // String startDate = String.format('%tF %<tT', new Date()) + " GMT"
			def timedur = (long)(((new Date()).time - jobs.get(0).executionStartTime.time) / 60000)
			xml.chart_data {
				xml.row { xml."null"(""); (timedur+1).times { xml.string("x"); xml.string("y") } }
				RemoteFunction.list().each { func ->
					def numPerTimeDur = []
					jobs.findAll { it.function.name == func.name }.each { job ->
						numPerTimeDur << (long)(job.executionTime / 60)
					}
					xml.row { 
						xml.string("${func.name}")
						numPerTimeDur.clone().unique().each { aDur ->
								def theNum = numPerTimeDur.count(aDur); xml.number("${aDur}"); xml.number("${theNum}")
						}
					}
				}
			}
			xml.legend_label( layout:'vertical', font:'arial', bold:'true', "size":'12', color:'ffffff', alpha:'50' )
			xml.live_update( url:'/scheduler/job/jobs?uniqueID=0.26440600+1128349620' )
			xml.chart_grid_h( alpha:'10', color:'000000', thickness:'1', type:'solid' )
			xml.chart_grid_v( alpha:'10', color:'000000', thickness:'1', type:'solid' )
			xml.chart_pref( line_thickness:'2', point_shape:'none', fill_shape:'false' )
			xml.chart_rect( x:'25', y:'15', width:'500', height:'300', positive_color:'000000', 
									positive_alpha:'30', negative_color:'ff0000', negative_alpha:'10' )
			xml.chart_type("scatter")
			xml.chart_value( position:'cursor', "size":'12', color:'ffffff', alpha:'75' )
			xml.draw {
				xml.text( color:'ffffff', alpha:'15', font:'arial', rotation:'-90', bold:'true', 
					"size":'50', x:'-5', y:'300', width:'300', height:'150', h_align:'center', v_align:'top', "jobDone" )
				xml.text( color:'000000', alpha:'15', font:'arial', rotation:'0', bold:'true', "size":'60', 
							x:'10', y:'10', width:'320', height:'300', h_align:'left', v_align:'bottom', "time" )
			}
			xml.legend_rect( x:'80', y:'50', width:'10', height:'10', margin:'10', fill_alpha:'50' )
			xml.series_color {
				xml.color("88ff00"); xml.color("ff8800"); xml.color("88ffee"); xml.color("ff88ee")
			}
		}
	}

	def makeStackColumnChart( allJobs, Closure filter, StringWriter writer, valueMap ) {
		def xml = new MarkupBuilder(writer)
		def jobs = allJobs.findAll { filter(it) }
		xml.chart {
			def maxNum = 100; 
			xml.axis_category( "size":'10', color:'000000', alpha:'90', font:'arial', 
									bold:'true', skip:'0', orientation:'horizontal')
			xml.axis_ticks( value_ticks:'true', category_ticks:'true', major_thickness:'2', minor_thickness:'1', 
									minor_count:'1', major_color:'000000', minor_color:'222222', position:'outside' )
			xml.axis_value( min:'0', max:maxNum, font:'arial', bold:'true', "size":'10', color:'000000', 
						alpha:'90', steps:10, prefix:'', suffix:'', decimals:'0', 
						separator:'', show_min:'true' )
			xml.chart_border( color:'000000', top_thickness:'2', 
							bottom_thickness:'2', left_thickness:'2', right_thickness:'2' )
			xml.chart_data {
				def jobIds = [:]; allJobs.each { jobIds[it.jid] = 0 }; jobs.each { jobIds[it.jid] = 1 }
				xml.row { 
						xml."null"()
						jobIds.each { 
							// if( it.key < 0 ) { println " [DBG] find jid:[$it] < 0 " }
							def id = (it.key >= 0)? it.key : -it.key+"c"
							xml.string("$id")
						}
				}
				xml.row {
						xml.string("queue")
						allJobs.each { 
							if ( jobIds[it.jid] ) {
								if( it.status != "canceled" ) {
										xml.number("${Math.abs(it.queuingTimeNow())}")
								} else { xml."null"() }
							} else { xml."null"() }
						}
				}
				xml.row { 
						xml.string("execute")
						allJobs.each {
							if( jobIds[it.jid] ) {
								def aTime = Math.abs(it.executionTimeNow())
								if( it.status == "finished" || it.status == "canceled" ) xml."null"()
								else if( aTime ) xml.number("${aTime}"); else xml."null"() 
							} else { xml."null"() }
						}
				}
				xml.row {
						xml.string("finish")
						allJobs.each {
							if( jobIds[it.jid] ) {
								def aTime = Math.abs(it.executionTimeNow())
								if( it.status == "finished" ) xml.number("${aTime}"); else xml."null"() 
							} else { xml."null"() }
						}
				}
				xml.row {
						xml.string("canceled")
						allJobs.each {
							if( jobIds[it.jid] ) {
								def aTime = Math.abs(it.executionTimeNow() + it.queuingTimeNow())
								if( it.status == "canceled" ) xml.number("${aTime}"); else xml."null"()
							} else { xml."null"() }
						}
				}
			}
			xml.legend_label( layout:'horizontal', font:'arial', bold:'true', "size":'12', color:'222222', alpha:'80' )
			// reload time delay: 2min
			xml.live_update( url:valueMap.live_update.link, delay:'120' )
			xml.chart_grid_h( alpha:'10', color:'000000', thickness:'1', type:'solid' )
			xml.chart_grid_v( alpha:'10', color:'000000', thickness:'1', type:'solid' )
			xml.chart_pref( line_thickness:'2', point_shape:'none', fill_shape:'false' )
			xml.chart_rect( x:valueMap.chart_rect.x, y:valueMap.chart_rect.y, 
							width:valueMap.chart_rect.width, height:valueMap.chart_rect.height, 
							positive_color:'ffffff', positive_alpha:'0', negative_color:'ff0000', negative_alpha:'0' )
			xml.chart_type("stacked column")
			xml.chart_value( position:'middle', "size":'10', color:'000000', alpha:'75' )
			int width = Integer.parseInt( valueMap.chart_rect.width )
			int height = Integer.parseInt( valueMap.chart_rect.height )
			xml.draw {
				xml.text( color:'222222', alpha:'15', font:'arial', rotation:'0', bold:'true', 
					"size":'50', x:5, y:5, v_align:'top', h_align:'left', "" )
				xml.text( color:'222222', alpha:'15', font:'arial', rotation:'0', bold:'true', 
					"size":'50', x:12, y:12, v_align:'top', h_align:'left', "" )
				xml.text( color:valueMap.text.color, alpha:valueMap.text.alpha, 
								font:'arial', rotation:'0', bold:'true', "size":valueMap.text.size, 
								x:valueMap.text.x, y:valueMap.text.y, h_align:'left', v_align:'top', valueMap.text.funcName )
			}
			xml.legend_rect( x:'65', y:'26', height:'20', margin:'5', fill_alpha:'75' )
			xml.series_color { valueMap.series_color.each { xml.color("${it}") } }
		}
	}

}
