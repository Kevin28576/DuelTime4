package com.kevin.dueltime4.data.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class ListStringTypeHandler extends BaseTypeHandler<List<String>> {

    protected static final String DELIMITER = " DUELTIME_LINE";
    protected static final String PREFIX = " DUELTIME_LIST ";

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, serialize(parameter));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return deserialize(rs.getString(columnName));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return deserialize(rs.getString(columnIndex));
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return deserialize(cs.getString(columnIndex));
    }

    protected static String serialize(List<String> list) {
        return PREFIX + String.join(DELIMITER, list);
    }

    protected static List<String> deserialize(String str) {
        return (str != null && !str.isEmpty()) ? Arrays.asList(str.substring(PREFIX.length()).split(DELIMITER)) : null;
    }
}