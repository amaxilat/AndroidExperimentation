CREATE DATABASE  IF NOT EXISTS `androidexperimentation` /*!40100 DEFAULT CHARACTER SET utf8 */;
USE `androidexperimentation`;
-- MySQL dump 10.13  Distrib 5.6.11, for Win32 (x86)
--
-- Host: localhost    Database: androidexperimentation
-- ------------------------------------------------------
-- Server version	5.6.13-log

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `experiments`
--

DROP TABLE IF EXISTS `experiments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `experiments` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(256) NOT NULL,
  `contextType` varchar(256) NOT NULL,
  `sensorDependencies` text NOT NULL,
  `fromTime` datetime DEFAULT NULL,
  `toTime` datetime DEFAULT NULL,
  `status` varchar(256) NOT NULL,
  `userID` int(11) DEFAULT NULL,
  `url` varchar(256) NOT NULL,
  `filename` varchar(256) NOT NULL,
  `description` text NOT NULL,
  `timestamp` bigint(20) NOT NULL
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `experiments`
--

LOCK TABLES `experiments` WRITE;
/*!40000 ALTER TABLE `experiments` DISABLE KEYS */;
INSERT INTO `experiments` VALUES (1,'Experiment1','org.ambientdynamix.contextplugins.ExperimentPlugin','org.ambientdynamix.contextplugins.GpsPlugin','2000-12-01 00:00:00','2000-12-01 00:00:00','',0,'http://83.212.110.88:8080/dynamixRepository/org.ambientdynamix.contextplugins.ExperimentPlugin_0.9.54.jar','org.ambientdynamix.contextplugins.ExperimentPlugin_0.9.54.jar');
/*!40000 ALTER TABLE `experiments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `plugins`
--

DROP TABLE IF EXISTS `plugins`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `plugins` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(256) NOT NULL,
  `contextType` varchar(256) NOT NULL,
  `runtimeFactoryClass` varchar(256) NOT NULL,
  `description` varchar(256) NOT NULL,
  `installUrl` varchar(256) NOT NULL,
  `filename` varchar(256) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `plugins`
--

LOCK TABLES `plugins` WRITE;
/*!40000 ALTER TABLE `plugins` DISABLE KEYS */;
INSERT INTO `plugins` VALUES (1,'plugs.xml','plugs.xml','plugs.xml','plugs.xml','http://83.212.110.88:8080/dynamixRepository/plugs.xml','plugs.xml'),(6,'GpsPlugin','org.ambientdynamix.contextplugins.GpsPlugin','org.ambientdynamix.contextplugins.GpsPlugin.PluginFactory','GpsPlugin','http://83.212.110.88:8080/dynamixRepository/org.ambientdynamix.contextplugins.GpsPlugin_0.9.54.jar','org.ambientdynamix.contextplugins.GpsPlugin_0.9.54.jar');
/*!40000 ALTER TABLE `plugins` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `results`
--

DROP TABLE IF EXISTS `results`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `results` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `experimentID` int(11) NOT NULL,
  `deviceID` int(11) NOT NULL,
  `message` text NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `results`
--

LOCK TABLES `results` WRITE;
/*!40000 ALTER TABLE `results` DISABLE KEYS */;
/*!40000 ALTER TABLE `results` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `smartphones`
--

DROP TABLE IF EXISTS `smartphones`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `smartphones` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `phoneID` int(11) NOT NULL,
  `sensorsRules` text,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=42 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `smartphones`
--

LOCK TABLES `smartphones` WRITE;
/*!40000 ALTER TABLE `smartphones` DISABLE KEYS */;
INSERT INTO `smartphones` VALUES (4,4,'org.ambientdynamix.contextplugins.batteryLevelPlugin,org.ambientdynamix.contextplugins.batteryTemperaturePlugin,org.ambientdynamix.contextplugins.GpsPlugin,org.ambientdynamix.contextplugins.WifiScanPlugin'),(11,11,''),(12,12,''),(13,13,'org.ambientdynamix.contextplugins.GpsPlugin,'),(14,14,''),(15,15,''),(16,16,''),(17,17,''),(18,18,''),(19,19,''),(20,20,'org.ambientdynamix.contextplugins.GpsPlugin,'),(21,21,'org.ambientdynamix.contextplugins.GpsPlugin,'),(22,22,''),(23,23,''),(24,24,'REACTIVE,'),(25,25,'REACTIVE,'),(26,26,''),(27,27,'REACTIVE,REACTIVE,'),(28,28,'REACTIVE,REACTIVE,'),(29,29,'REACTIVE,REACTIVE,'),(30,30,'REACTIVE,REACTIVE,'),(31,31,'REACTIVE,REACTIVE,'),(32,32,'GpsPlugin,'),(33,33,'GpsPlugin,'),(34,34,'GpsPlugin,'),(35,35,'org.ambientdynamix.contextplugins.GpsPlugin,'),(36,36,'org.ambientdynamix.contextplugins.GpsPlugin,'),(37,37,'org.ambientdynamix.contextplugins.GpsPlugin,'),(38,38,'org.ambientdynamix.contextplugins.ExperimentPlugin,org.ambientdynamix.contextplugins.GpsPlugin,'),(39,39,'org.ambientdynamix.contextplugins.GpsPlugin,'),(40,40,'org.ambientdynamix.contextplugins.ExperimentPlugin,org.ambientdynamix.contextplugins.GpsPlugin,'),(41,41,'');
/*!40000 ALTER TABLE `smartphones` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2013-10-15 11:51:56
