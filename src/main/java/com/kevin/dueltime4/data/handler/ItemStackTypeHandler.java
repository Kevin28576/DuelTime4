package com.kevin.dueltime4.data.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeException;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;

public class ItemStackTypeHandler extends BaseTypeHandler<ItemStack> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, ItemStack parameter, JdbcType jdbcType) throws SQLException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(parameter);
            dataOutput.flush();
            dataOutput.close();
            byte[] bytes = outputStream.toByteArray();
            String base64 = Base64.getEncoder().encodeToString(bytes);
            ps.setString(i, base64);
        } catch (IOException e) {
            throw new TypeException("Error serializing ItemStack to byte array", e);
        }
    }

    @Override
    public ItemStack getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return getItemStackByBytes(Base64.getDecoder().decode(rs.getString(columnName)));
    }

    @Override
    public ItemStack getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return getItemStackByBytes(Base64.getDecoder().decode(rs.getString(columnIndex)));
    }

    @Override
    public ItemStack getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return getItemStackByBytes(Base64.getDecoder().decode(cs.getString(columnIndex)));
    }

    private ItemStack getItemStackByBytes(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack itemStack = (ItemStack) dataInput.readObject();
            dataInput.close();

            return itemStack;
        } catch (IOException | ClassNotFoundException e) {
            throw new TypeException("Error deserializing ItemStack from byte array", e);
        }
    }
}
