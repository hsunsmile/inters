/*
SQLyog Enterprise - MySQL GUI v6.07
Host - 5.0.45-community-nt : Database - test
*********************************************************************
Server version : 5.0.45-community-nt
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

create database if not exists `test`;

USE `test`;

/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;

/*Table structure for table `industry` */

DROP TABLE IF EXISTS `industry`;

CREATE TABLE `industry` (
  `industryID` int(10) unsigned NOT NULL auto_increment,
  `industryName` varchar(40) NOT NULL default '',
  PRIMARY KEY  (`industryID`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=latin1;

/*Data for the table `industry` */

insert  into `industry`(`industryID`,`industryName`) values (1,'automotive'),(2,'computer'),(3,'finance'),(4,'food'),(5,'manufacturing'),(6,'medical'),(7,'retail'),(8,'services');

/*Table structure for table `stock` */

DROP TABLE IF EXISTS `stock`;

CREATE TABLE `stock` (
  `companyID` int(10) unsigned NOT NULL auto_increment,
  `company` varchar(40) default '',
  `price` float default NULL,
  `change` float default NULL,
  `pctChange` float default NULL,
  `lastChange` datetime default NULL,
  `industryID` int(10) default NULL,
  `risk` enum('low','medium','high') default NULL,
  `stars` enum('1','2','3','4','5') default NULL,
  `check` tinyint(1) default NULL,
  PRIMARY KEY  (`companyID`)
) ENGINE=InnoDB AUTO_INCREMENT=2036 DEFAULT CHARSET=latin1 ROW_FORMAT=DYNAMIC;

/*Data for the table `stock` */

insert  into `stock`(`companyID`,`company`,`price`,`change`,`pctChange`,`lastChange`,`industryID`,`risk`,`stars`,`check`) values (1,'Alcoa',23.33,0.42,1.47,'2007-10-02 00:00:00',5,'low','5',0),(2,'General Electric Company',22,-0.08,-0.23,'2007-10-02 00:00:00',5,'high','3',1),(144,'Altria Group Inc',10,0.28,0.34,'2007-02-02 00:00:00',5,'low','4',1),(145,'American Express Company',51,0.01,0.02,'2007-10-26 00:00:00',4,'low','2',1),(146,'AT&T',31.63,-0.48,-1.54,'2007-12-25 00:00:00',8,'medium','3',1),(148,'Caterpillar Inc.',67.27,0.92,1.39,'2007-10-02 00:00:00',5,'low','3',1),(149,'E.I. du Pont de Nemours and Company',10,0.51,1.28,'2007-10-02 00:00:00',5,'low','2',0),(150,'Exxon Mobil Corp',68.1,-0.43,-0.64,'2007-10-02 00:00:00',5,'medium','2',0),(152,'General Motors Corporation',30.27,1.09,3.74,'2007-10-02 00:00:00',1,'high','4',1),(154,'Honeywell Intl Inc',38.77,0.05,0.13,'2007-10-02 00:00:00',5,'high','5',0),(155,'Intel Corporation',19.88,0.31,1.58,'2007-10-02 00:00:00',2,'medium','2',0),(156,'International Business Machines',81.41,0.44,0.54,'2007-10-02 00:00:00',2,'medium','1',0),(157,'Johnson & Johnson',64.72,0.06,0.09,'2007-10-02 00:00:00',6,'high','2',0),(158,'JP Morgan & Chase & Co',45.73,0.07,0.15,'2007-10-02 00:00:00',3,'low','3',0),(162,'The Coca-Cola Company',45.07,0.26,0.58,'2007-10-02 00:00:00',4,'medium','2',0),(163,'The Procter & Gamble Company',61.91,0.01,0.02,'2007-10-02 00:00:00',5,'low','4',0),(164,'United Technologies Corporation',63.26,0.55,0.88,'2007-10-02 00:00:00',2,'high','5',1),(165,'Verizon Communications',35.57,0.39,1.11,'2007-10-02 00:00:00',8,'medium','4',0),(166,'3m Company',44,0.02,0.03,'2007-08-01 00:00:00',5,'low','1',1),(2009,'Boeing Co.',75.43,0.53,0.71,'2007-08-01 00:00:00',5,'low','3',1),(2015,'Hewlett-Packard Co.',36.54,-0.03,-0.08,'2007-08-01 00:00:00',2,'low','1',1),(2021,'McDonald\'s Corporation',36.76,0.86,2.4,'2007-08-01 00:00:00',4,'high','5',0),(2022,'Microsoft Corporation',25.84,0.14,0.54,'2007-08-01 00:00:00',2,'high','3',1),(2023,'Pfizer Inc',27.96,0.4,1.45,'2007-08-01 00:00:00',6,'high','5',0),(2030,'Citigroup',49.37,0.02,0.04,'2007-10-05 00:00:00',3,'high','3',0),(2031,'Merck & Co., Inc.',40.96,0.41,1.01,'2007-10-09 00:00:00',6,'medium','2',0),(2032,'Walt Disney Company (The) (Holding Compa',29.89,0.24,0.81,'2007-10-05 00:00:00',8,'low','2',1),(2033,'Wal-Mart Stores, Inc.',45.45,0.73,1.63,'2007-05-09 00:00:00',7,'medium','2',1),(2034,'American International Group, Inc.',10,0.31,0.49,'2007-06-09 00:00:00',2,'high','2',1),(2035,'The Home Depot',34.64,0.35,1.02,'2007-08-05 00:00:00',7,'medium','4',0);

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
