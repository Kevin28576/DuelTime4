package com.kevin.dueltime4.data.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LocationTypeHandler extends BaseTypeHandler<Location> {
    protected static final String PREFIX = " DUELTIME LOCATION ";

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Location parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, serialize(parameter));
    }

    @Override
    public Location getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return deserialize(rs.getString(columnName));
    }

    @Override
    public Location getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return deserialize(rs.getString(columnIndex));
    }

    @Override
    public Location getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return deserialize(cs.getString(columnIndex));
    }

    protected static String serialize(Location location) {
        return PREFIX + location.getWorld().getName() + "," + location.getX() + "," + location.getY() + "," + location.getZ() + "," + location.getYaw() + "," + location.getPitch();
    }

    protected static Location deserialize(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        String[] parts = str.substring(PREFIX.length()).split(",");
        if (parts.length != 6) {
            throw new TypeException("Invalid location string: " + str);
        }
        World world = Bukkit.getWorld(parts[0]);
        double x = Double.parseDouble(parts[1]);
        double y = Double.parseDouble(parts[2]);
        double z = Double.parseDouble(parts[3]);
        float yaw = Float.parseFloat(parts[4]);
        float pitch = Float.parseFloat(parts[5]);
        return new Location(world, x, y, z, yaw, pitch);
    }
    /*@Override
    public void setNonNullParameter(PreparedStatement ps, int i, Location parameter, JdbcType jdbcType) throws SQLException {
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
            throw new TypeException("Error serializing Location to byte array", e);
        }
    }

    @Override
    public Location getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return getLocationByBytes(Base64.getDecoder().decode(rs.getString(columnName)));
    }

    @Override
    public Location getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return getLocationByBytes(Base64.getDecoder().decode(rs.getString(columnIndex)));
    }

    @Override
    public Location getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return getLocationByBytes(Base64.getDecoder().decode(cs.getString(columnIndex)));
    }

    private Location getLocationByBytes(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            Object object = dataInput.readObject();
            if (object instanceof Location) {
                return (Location) object;
            } else {
                System.err.println("Deserialized object is not an instance of Location: " + object.getClass().getName());
                throw new TypeException("Deserialized object is not an instance of Location");
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            throw new TypeException("Error deserializing Location from byte array", e);
        }
    }*/
}
