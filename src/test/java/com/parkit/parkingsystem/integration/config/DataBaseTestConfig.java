package com.parkit.parkingsystem.integration.config;

import com.parkit.parkingsystem.config.DataBaseConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;

public class DataBaseTestConfig extends DataBaseConfig {

    private static final Logger logger = LogManager.getLogger("DataBaseTestConfig");

    private final String url;
    private final String username;
    private final String password;

    // Constructeur par défaut qui marche avec la configuration initiale, en local par défault
    public DataBaseTestConfig() {
        this.url = "jdbc:mysql://localhost:3306/prod?serverTimezone=Europe/Paris";
        this.username = "root";
        this.password = "rootroot";
    }

    //Constructeur pour le dockerTestContainer
    // On declare mais on ne sait pas ou se trouve le conteneur
    // (c'est testContainers qui va le générer aléatoirement à chaque lancement
    // Donc il faudra refaire le lien ensuite côté classe de test
    public DataBaseTestConfig(String url, String username, String password){
        this.url = url;
        this.username = username;
        this.password = password;
    }


    public Connection getConnection() throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(url, username, password);
    }

    public void closeConnection(Connection con){
        if(con!=null){
            try {
                con.close();
                logger.info("Closing DB connection");
            } catch (SQLException e) {
                logger.error("Error while closing connection",e);
            }
        }
    }
    public void closePreparedStatement(PreparedStatement ps) {
        if(ps!=null){
            try {
                ps.close();
                logger.info("Closing Prepared Statement");
            } catch (SQLException e) {
                logger.error("Error while closing prepared statement",e);
            }
        }
    }

    public void closeResultSet(ResultSet rs) {
        if(rs!=null){
            try {
                rs.close();
                logger.info("Closing Result Set");
            } catch (SQLException e) {
                logger.error("Error while closing result set",e);
            }
        }
    }
}
