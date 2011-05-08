<?php


class ExtTableFilter extends DatabaseFilter {
	//private static final DateFormat DF = SimpleDateFormat.getDateInstance(DateFormat.SHORT);
	
	public function __construct(Request $request, $offsetParam="start", $limitParam="limit", $sortParam="sort", $sortDirParam="dir", $filtersParam="filters")  {
		$this->offset     = $offsetParam != null && $request->getParameter($offsetParam) != null ? intval($request->getParameter($offsetParam)) : -1;
		$this->limit      = $offsetParam != null && $request->getParameter($limitParam) != null ? intval($request->getParameter($limitParam)) : -1;
		
		$this->orderField = $request->getParameter($sortParam);
		$this->orderDir   = $request->getParameter($sortDirParam) != null ? $request->getParameter($sortDirParam) : null;		
		$this->parseWhereClause($request);
	}

	private function parseWhereClause(Request $request)  {
		
		$this->where = array();
				
		
		$i = 0;
		$filters = $request->getParameter("filter");
		if(count($filters) > 0){
			while(isset($filters[$i]) && isset($filters[$i]['field'])){
				$filter = $filters[$i];
				
				
				$field = $filter['field'];
				$type  = isset($filter['data']['type'])?$filter['data']['type']:null;
				$value = isset($filter['data']['value'])?$filter['data']['value']:null;
				
				if("string" == $type){
					$this->where[]= (new StringWhereClause($field, $value, Comparison::LIKE()));
				} 
				else if("list" == $type){
					
					$value = explode(',', $value);
					

					$this->where[]= (new ListWhereClause($field, $value, true));
				}
				else if("numeric" == ($type)) {
					$comp = Comparison::valueOf(strtoupper($filter['data']['comparison']));
					$this->where[]= (new NumericWhereClause($field, floatval($value), $comp));
				} else if("date" == $type){
					$comp = Comparison::valueOf(strtoupper($filter['data']['comparison']));
					$this->where[]= (new DateWhereClause($field, $value, $comp));
				} else if("boolean" == ($type)){
					$this -> where[]= (new BooleanWhereClause($field, intval($value)==1 || $value == 'true'));
				} else if("null" == ($type)){
					$this -> where[]= (new NullWhereClause($field, intval($value)==1));
				}
				
				
				$i++;
			}
		}
	}
}
?>