<?php session_start(); ?>	

<?php require_once('/var/www/androidDistributed/config.php'); ?>

<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title> View Experiment </title>
<link rel="stylesheet" type="text/css" href="style/view.css" />

</head>
<body>

<?php include("includes/header.html"); ?>

<?php
if(isset($_SESSION['username']))
{
	echo('<div id="wrapper_options">
		<div id="options">
			<p> Hi, <b><font size="3">'. $_SESSION['username'].'</font></b></p>
			<a href="profile.php">View Profile</a>
			</br>
			<a href="logout.php">Logout</a>
	     	</div>
	     </div>');
}
?>

<?php

require_once(MYSQL); 


$id = $_GET['experiment_id'];


$dbc = db_connect(); // connect to db
							// query to db to fetch experiment infos
$query="select contextType,user_email,name,sensorDependencies,timeDependencies,expires,status,url from experiments where id=?";
				// prepare query
$stmt = mysqli_prepare($dbc, $query);
  				// Bind Parameters [s for string]
mysqli_stmt_bind_param($stmt, "i", $id);
  			// execute statement
mysqli_stmt_execute($stmt);
		    				// bind result variables 
mysqli_stmt_bind_result($stmt, $contextType,$user_email,$name,$sensorDependencies,$timeDependencies,$expires,$status,$url);

 			// fetch values 
if( !(mysqli_stmt_fetch($stmt)) )
{
	echo '<p class="error"> Sorry somthing goes wrong </p> ';
	die();
}

$experiment_infos = array( 'contextType' => $contextType, 'user_email' => $user_email, 'name' => $name,
					'sensorDependencies' => $sensorDependencies, 'timeDependencies' => $timeDependencies, 
					'expires' => $expires, 'status' => $status,
					 'url' => $url);
		// close statement
mysqli_stmt_close($stmt);
	
?>

<?php
echo('<div id="wrapper_up">
	<div id="image_view">');
	echo('<img src="images/plugins.png">');
	echo('</div>');

echo('</div>');
?>

<?php
echo('<div id="experiment_infos">');
if(isset($experiment_infos))
{
	echo('<p><b><font size="2"/> ContextType: </b></font> <font size="3">' .$experiment_infos['contextType']. '</font></p>
	<p><b><font size="2">User email:  </b></font><font size="3">'.$experiment_infos['user_email'].'</font></p>
	<p><b><font size="2"> Experiment name: </b></font><font size="3">'.$experiment_infos['name'].'</font></p>
	<p><b><font size="2"> sensor dependencies: </b></font><font size="3">'.$experiment_infos['sensorDependencies'].'</font></p>
	<p><b><font size="2"> time dependencies: </b></font><font size="3">'.$experiment_infos['timeDependencies'].'</font></p>
	<p><b><font size="2"> expires: </b></font><font size="3">'.$experiment_infos['expires'].'</font></p>
	<p><b><font size="2"> status: </b></font><font size="3">'.$experiment_infos['status'].'</font></p>
	<p><b><font size="2"> url: </b></font><font size="3">'.$experiment_infos['url'].'</font></p>');

	echo '<a href="view_report.php?experiment_id='.$id.'"> <img src="images/report.png"> </a>';
}
echo('	</div>');
?>

<?php include('includes/footer.html'); ?> 

</body>
</html>
