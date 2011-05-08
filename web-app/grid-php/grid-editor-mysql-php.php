<?php   

//database parameters
$user='test';   //user
$pw='test';     //user password
$db='test';     //name of database
$table='stock'; //name of table data stored in

$taxRate = 0.06;
    
//make database connection
$connection = mysql_connect("localhost", $user, $pw) or
   die("Could not connect: " . mysql_error());
mysql_select_db($db) or die("Could not select database");

$task = ($_POST['task']) ? ($_POST['task']) : null;

//switchboard for the CRUD task requested
switch($task){
    case "create":
        addData();
        break;
    case "readStock":
        showData('stock');
        break;
    case "readIndustry":
        getData('industry');
        break;
    case "update":
        saveData();
        break;
    case "delete":
        removeData();
        break;
    case "calcTax":
        getTax();
        break;
    default:
        echo "{failure:true}";
        break;
}//end switch

    
function showData($table) 
{
    global $taxRate;
    
     /* By specifying the start/limit params in ds.load 
      * the values are passed here
      * if using ScriptTagProxy the values will be in $_GET
      * if using HttpProxy      the values will be in $_POST (or $_REQUEST)
      * the following two lines check either location, but might be more
      * secure to use the appropriate one according to the Proxy being used
      */
    $start = (integer) (isset($_POST['start']) ? $_POST['start'] : $_GET['start']);
    $end = (integer) (isset($_POST['limit']) ? $_POST['limit'] : $_GET['limit']);  
    
    $sql_count = 'SELECT * FROM ' . $table;
    $sql = $sql_count . ' LIMIT ' . $start . ', '. $end;
    
    $result_count = mysql_query($sql_count);
    $rows = mysql_num_rows($result_count);
    
    $result = mysql_query($sql);
    
    while($rec = mysql_fetch_array($result, MYSQL_ASSOC)){
        //these lines are to populate the tax column for the initial display
        //of the grid, any updates to the price are handled by another function below
        $price = $rec['price'];
        $rec['tax'] = round($price * ($taxRate),2);
        
        $arr[] = $rec;
    };

    if (version_compare(PHP_VERSION,"5.2","<"))
    {    
        require_once("./JSON.php"); //if php<5.2 need JSON class
        $json = new Services_JSON();//instantiate new json object
        $data=$json->encode($arr);  //encode the data in json format
    } else
    {
        $data = json_encode($arr);  //encode the data in json format
    }

    /* If using ScriptTagProxy:  In order for the browser to process the returned
       data, the server must wrap te data object with a call to a callback function,
       the name of which is passed as a parameter by the ScriptTagProxy. (default = "stcCallback1001")
       If using HttpProxy no callback reference is to be specified*/
    $cb = isset($_GET['callback']) ? $_GET['callback'] : '';
       
     echo $cb . '({"total":"' . $rows . '","results":' . $data . '})';

}//end showData



function getData($table) 
{

    $sql = 'SELECT * FROM ' . $table;
    $result = mysql_query($sql);

    while($rec = mysql_fetch_array($result, MYSQL_ASSOC)){
        $arr[] = $rec;
    };

    if (version_compare(PHP_VERSION,"5.2","<"))
    {    
        require_once("./JSON.php"); //if php<5.2 need JSON class
        $json = new Services_JSON();//instantiate new json object
        $data=$json->encode($arr);  //encode the data in json format
    } else
    {
        $data = json_encode($arr);  //encode the data in json format
    }

    /* If using ScriptTagProxy:  In order for the browser to process the returned
       data, the server must wrap te data object with a call to a callback function,
       the name of which is passed as a parameter by the ScriptTagProxy. (default = "stcCallback1001")
       If using HttpProxy no callback reference is to be specified*/
    $cb = isset($_GET['callback']) ? $_GET['callback'] : '';
       
     echo $cb . '({"results":' . $data . '})';

}//end getData

                
function saveData()
{
    /*
     * $key:   db primary key label
     * $id:    db primary key value
     * $field: column or field name that is being updated (see data.Record mapping)
     * $value: the new value of $field
     */ 

    global $table;
    $key = $_POST['key'];
    $id    = (integer) mysql_real_escape_string($_POST['keyID']);
    $field = $_POST['field'];
    $value = $_POST['value'];
    $newRecord = $id == 0 ? 'yes' : 'no';                   
    
    //should validate and clean data prior to posting to the database

    if ($newRecord == 'yes'){
        //INSERT INTO `stock` (`company`) VALUES ('a new company');
        $query = 'INSERT INTO `'.$table.'` (`'.$field.'`) VALUES (\''.$value.'\')';
    } else {
        $query = 'UPDATE `'.$table.'` SET `'.$field.'` = \''.$value.'\' WHERE `'.$key.'` = '.$id;
    }

    //save data to database                                                    
    $result = mysql_query($query);
    $rows = mysql_affected_rows();
    
    if ($rows > 0) {
        if($newRecord == 'yes'){
            $newID = mysql_insert_id();
            echo "{success:true, newID:$newID}";
        } else {
            echo "{success:true}";
        }
    } else {
        echo "{success:false}"; //if we want to trigger the false block we should redirect somewhere to get a 404 page
    }
}//end saveData


function removeData()
{
    /*
     * $key:   db primary key label
     * $id:    db primary key value
     */ 

    global $table;
    $key = $_POST['key'];
    $arr    = $_POST['companyID'];
    $count = 0;


    if (version_compare(PHP_VERSION,"5.2","<"))
    {    
        require_once("./JSON.php"); //if php<5.2 need JSON class
        $json = new Services_JSON();//instantiate new json object
        $selectedRows = $json->decode(stripslashes($arr));//decode the data from json format
    } else
    {
        $selectedRows = json_decode(stripslashes($arr));//decode the data from json format
    }

    //should validate and clean data prior to posting to the database
    foreach($selectedRows as $row_id)
    {
        $id = (integer) $row_id;
        $query = 'DELETE FROM `'.$table.'` WHERE `'.$key.'` = '.$id;
        $result = mysql_query($query); //returns number of rows deleted
        if ($result) $count++;
    }
    
    if ($count) { //only checks if the last record was deleted, others may have failed

        /* If using ScriptTagProxy:  In order for the browser to process the returned
           data, the server must wrap te data object with a call to a callback function,
           the name of which is passed as a parameter by the ScriptTagProxy. (default = "stcCallback1001")
           If using HttpProxy no callback reference is to be specified*/
        $cb = isset($_GET['callback']) ? $_GET['callback'] : '';
           
        $response = array('success'=>$count, 'del_count'=>$count);


        if (version_compare(PHP_VERSION,"5.2","<"))
        {    
            $json_response = $json->encode($response);
        } else
        {
            $json_response = json_encode($response);
        }

        echo $cb . $json_response;
//        echo '{success: true, del_count: '.$count.'}';
    } else {
        echo '{failure: true}';
    }
}//end saveData

/**
 * Get Tax
 * Determine tax based on price
 */
function getTax()
{                              
    $price = $_POST['price'];

    if ($price >= 0) {
        global $taxRate;

        $tax = round($price * ($taxRate),2);

        $cb = isset($_GET['callback']) ? $_GET['callback'] : '';

        $response = array('success'=>'true', 'tax'=>$tax);

        if (version_compare(PHP_VERSION,"5.2","<"))
        {    
            require_once("./JSON.php"); //if php<5.2 need JSON class
            $json = new Services_JSON();//instantiate new json object
            $json_response = $json->encode($response);
        } else
        {
            $json_response = json_encode($response);
        }

        echo $cb . $json_response;
    } else {
        echo '{failure: true}';
    }
}//end getTotal
?>