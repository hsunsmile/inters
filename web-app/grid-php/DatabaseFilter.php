<?php

final class Direction {
	const ASC = 0;
	const DESC = 1;
}

final class Comparison {
	
public static function GT(){return new Comparison(">");}
public static function LT(){return new Comparison("<");}
public static function GTE(){return new Comparison(">=");}
public static function LTE(){return new Comparison("<=");}
public static function EQ(){return new Comparison("=");}
public static function NEQ(){return new Comparison("!=");}
public static function LIKE(){return new Comparison("LIKE");}
public static function IN(){return new Comparison("IN");}
public static function NOTIN(){return new Comparison("NOT IN");}

	
	private $sql;
	public function __construct($sql){
		$this->sql = $sql;
	}
	
	
	public static function valueOf($string){
		
		$methods = array('GT', 'LT', 'GTE', 'LTE', 'EQ', 'NEQ', 'LIKE', 'IN', 'NOTIN');
		$string = strtoupper($string);
		if(in_array($string, $methods))
			return Comparison::$string();
		
		throw new Exception("comparison ".$string." not found");
	}
	
	public function __toString(){
		return $this->sql;
	}
}

abstract class WhereClause {
	public $field;
	public /*Comparison*/ $comparison;
	public $value;
	
	public function __construct($field, $value, Comparison $comparison) {
			$this->field      = $field;
			$this->value      = $value;
			$this->comparison = $comparison;
	}
		
	public abstract function setFieldValue(Statement $stmt, $paramOffset);	
}


class ListWhereClause extends WhereClause {
		public function __construct($field, Array $value, $inclusive){
			parent::__construct($field, $value, $inclusive ? Comparison::IN() : Comparison::NOTIN());
		}
		
		public function __toString(){
			if(count($this->value) > 0){
				
				$string = "";
				$string.= $this->field;
				$string.= ' ';
				$string.=$this->comparison;
				$string.=" (";
				for($i=0, $len=count($this->value); $i<$len; $i++){
					$string.= "?";
					
					if($i < $len - 1)
						$string.=',';
				}
				$string.=')';
				
				return $string;
			} else {
				return "0";
			}
		}
		
		public function setFieldValue(Statement $stmt, $paramOffset) {
			foreach($this->value as $val)
				$stmt->setString($paramOffset++, $val);			
			return $paramOffset;
		}
	}

	
  class NumericWhereClause extends WhereClause {
		public function __construct($field, $value, Comparison $comp){
			parent::__construct($field, $value, $comp);
		}
		
		public function __toString(){
			return $this->field . " " . $this->comparison . " ?"; 
		}
		
		public function setFieldValue(Statement $stmt, $paramOffset){
			$stmt->setFloat($paramOffset++, $this->value);			
			return $paramOffset;
		}
	}


class StringWhereClause extends WhereClause{
		public function __construct($field, $value, Comparison $comparison) {
			parent::__construct($field, $value, $comparison);
		}
		
		public function __toString(){
			return $this->field . " " . $this->comparison . " ?"; 
		}
		
		public function setFieldValue(Statement $stmt, $paramOffset) {
			$stmt -> setString($paramOffset++, '%' . $this->value . '%');			
			return $paramOffset;
		}
	}
	
	
class DateWhereClause extends WhereClause {
		public function __construct($field, $value, Comparison $comp){
			parent::__construct($field, $value, $comp);
		}
		
		public function __toString(){
			switch($this->comparison->__toString()){
				case ">": case ">=":
				case "<": case "<=":
					return "TIMESTAMPDIFF(DAY, ?, " . $this->field . ") " . $this->comparison . " 0";
				case "=":
					return "DATE_FORMAT(" . $this->field . ", '%Y-%m-%d') = DATE_FORMAT(?, '%Y-%m-%d')";
				default:
					throw new RuntimeException("Invalid comparison type for date field: " . $comparison);
			}
		}
		
		public function setFieldValue(Statement $stmt, $paramOffset)  {
			$stmt -> setDate($paramOffset++, $this->value);			
			return $paramOffset;
		}
	}
	
	
class BooleanWhereClause extends WhereClause {
		public function __construct($field, $value) {
			parent::__construct($field, $value, Comparison::EQ());
		}
		
		public function __toString(){
			return $this->field . " = ?";
		}
		
		public function setFieldValue(Statement $stmt, $paramOffset)  {
			$stmt->setBoolean($paramOffset++, $this->value);			
			return $paramOffset;
		}
	}
	
class NullWhereClause extends WhereClause {
		public function __construct($field, $isNull) {
			parent::__construct($field, $isNull, Comparison::EQ());
		}
		
		public function __toString(){
			return $this->value ? $this->field . " IS NULL" : " IS NOT NULL";
		}
		
		public function setFieldValue(Statement $stmt, $paramOffset)  {
			return $paramOffset;
		}
	}	

	
 class DatabaseFilter {
		
	public /*List<WhereClause>*/ $where;
	public /*String*/    $orderField;
	public /*Direction*/ $orderDir;
	public $offset;
	public $limit;
	
}
?>