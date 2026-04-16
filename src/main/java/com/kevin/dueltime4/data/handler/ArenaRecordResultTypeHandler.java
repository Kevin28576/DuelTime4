package com.kevin.dueltime4.data.handler;

import com.kevin.dueltime4.data.pojo.ClassicArenaRecordData;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeException;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ArenaRecordResultTypeHandler extends BaseTypeHandler<ClassicArenaRecordData.Result> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, ClassicArenaRecordData.Result parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.name());
    }

    @Override
    public ClassicArenaRecordData.Result getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return ClassicArenaRecordData.Result.valueOf(rs.getString(columnName));
    }

    @Override
    public ClassicArenaRecordData.Result getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return ClassicArenaRecordData.Result.valueOf(rs.getString(columnIndex));
    }

    @Override
    public ClassicArenaRecordData.Result getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return ClassicArenaRecordData.Result.valueOf(cs.getString(columnIndex));
    }
}
