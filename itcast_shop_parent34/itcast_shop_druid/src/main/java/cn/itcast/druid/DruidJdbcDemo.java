package cn.itcast.druid;

import java.sql.*;
import java.util.Properties;

/**
 * 使用jdbc的方式连接Druid
 */
public class DruidJdbcDemo {
    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        // 1. 加载Druid JDBC驱动
        Class.forName("org.apache.calcite.avatica.remote.Driver");
        // 2. 获取Druid JDBC连接
        Connection connection = DriverManager.getConnection("jdbc:avatica:remote:url=http://node03:8888/druid/v2/sql/avatica/", new Properties());

        // 3. 构建SQL语句
        String sql = "SELECT user, sum(views) as view_count FROM \"metrics-kafka\" GROUP BY 1 ORDER BY 1";

        // 4. 构建Statement，执行SQL获取结果集
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(sql);

        // 5. 迭代ResultSet
        while(resultSet.next()) {
            String user = resultSet.getString("user");
            long view_count = resultSet.getLong("view_count");
            System.out.println(user + " -> " + view_count);
        }

        // 6. 关闭Druid连接
        resultSet.close();
        statement.close();
        connection.close();
    }
}
