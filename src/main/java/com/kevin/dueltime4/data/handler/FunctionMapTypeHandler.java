package com.kevin.dueltime4.data.handler;

import com.kevin.dueltime4.util.UtilFormat;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FunctionMapTypeHandler extends BaseTypeHandler<HashMap<String, Object[]>> {
    protected static String DELIMITER_ITEM = " DUELTIME_ITEM ";
    protected static String DELIMITER_KV = " DUELTIME_KV ";
    protected static String DELIMITER_ELEMENT = " DUELTIME_ELEMENT ";

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, HashMap<String, Object[]> parameter, JdbcType jdbcType) throws SQLException {
        try {
            StringBuilder serializedParameter = new StringBuilder();
            parameter.forEach((key, valueArr) -> {
                serializedParameter.append(key).append(DELIMITER_KV);
                String valueSerialized = Arrays.stream(valueArr)
                        .map(value -> {
                            if (value instanceof Location) {
                                return LocationTypeHandler.serialize((Location) value);
                            } else if (value instanceof List && !((List<?>) value).isEmpty() && (((List<?>) value).get(0) instanceof String)) {
                                return ListStringTypeHandler.serialize((List<String>) value);
                            } else {
                                try {
                                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                    BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
                                    dataOutput.writeObject(value);
                                    dataOutput.flush();
                                    dataOutput.close();
                                    byte[] bytes = outputStream.toByteArray();
                                    return Base64.getEncoder().encodeToString(bytes);
                                } catch (IOException e) {
                                    throw new UncheckedIOException(e);
                                }
                            }
                        })
                        .collect(Collectors.joining(DELIMITER_ELEMENT));
                serializedParameter.append(valueSerialized).append(DELIMITER_ITEM);
            });
            if (serializedParameter.length() > 0) {
                serializedParameter.setLength(serializedParameter.length() - DELIMITER_ITEM.length());
            }
            ps.setString(i, serializedParameter.toString());
        } catch (UncheckedIOException e) {
            throw new TypeException("Error serializing Map to byte array", e.getCause());
        }
    }

    @Override
    public HashMap<String, Object[]> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return deserializeFunctionMap(rs.getString(columnName));
    }

    @Override
    public HashMap<String, Object[]> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return deserializeFunctionMap(rs.getString(columnIndex));
    }

    @Override
    public HashMap<String, Object[]> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return deserializeFunctionMap(cs.getString(columnIndex));
    }

    private HashMap<String, Object[]> deserializeFunctionMap(String str) {
        if (str == null || str.isEmpty()) return null;
        try {
            HashMap<String, Object[]> parameter = new HashMap<>();
            String[] kvArr = str.split(DELIMITER_ITEM);
            for (String kv : kvArr) {
                String[] keyAndValue = kv.split(DELIMITER_KV, 2);
                String key = keyAndValue[0];
                String[] valurStrArr = keyAndValue[1].split(DELIMITER_ELEMENT);
                Object[] valueArr = new Object[valurStrArr.length];
                for (int i = 0; i < valurStrArr.length; i++) {
                    String valueStr = valurStrArr[i];
                    Object value;
                    if (valueStr.startsWith(ListStringTypeHandler.PREFIX)) {
                        value = ListStringTypeHandler.deserialize(valueStr);
                    } else if (valueStr.startsWith(LocationTypeHandler.PREFIX)) {
                        value = LocationTypeHandler.deserialize(valueStr);
                    } else {
                        ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(valueStr));
                        BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
                        value = dataInput.readObject();
                        dataInput.close();
                    }
                    valueArr[i] = value;
                }
                parameter.put(key, valueArr);
            }
            return parameter;
        } catch (IOException | ClassNotFoundException e) {
            throw new TypeException("Error deserializing Map from byte array", e);
        }
    }
}
