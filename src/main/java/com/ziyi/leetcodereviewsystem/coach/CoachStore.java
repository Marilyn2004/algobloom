package com.ziyi.leetcodereviewsystem.coach;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import java.sql.*;

@Repository
public class CoachStore {
    final JdbcTemplate jdbc;
    private final JsonMapper json = JsonMapper.builder().build();
    private static final Set<String> OWNED = Set.of("conversations","drafts","sessions","items","attempts","journals","memories","messages","changes");
    public CoachStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public String json(Object value) { return json.writeValueAsString(value); }
    public Object parse(String value) { return json.readValue(value, Object.class); }
    public List<Map<String,Object>> rows(String sql,Object... args) {
        return jdbc.query(sql,(rs,index)->{
            Map<String,Object> row=new LinkedHashMap<>(); var meta=rs.getMetaData();
            for(int i=1;i<=meta.getColumnCount();i++) {
                String column=meta.getColumnLabel(i); StringBuilder key=new StringBuilder(); boolean upper=false;
                for(char c:column.toCharArray()) {if(c=='_') {upper=true;continue;} key.append(upper?Character.toUpperCase(c):c);upper=false;}
                Object v=rs.getObject(i);
                if(v!=null && (meta.getColumnTypeName(i).equals("jsonb")||meta.getColumnTypeName(i).equals("json"))) v=parse(v.toString());
                else if(v instanceof java.sql.Timestamp t) v=t.toInstant().toString();
                else if(v instanceof java.time.OffsetDateTime t) v=t.toInstant().toString();
                else if(v instanceof UUID) v=v.toString();
                row.put(key.toString(),v);
            } return row;
        },args);
    }
    public Map<String,Object> one(String sql,Object...args) { var rows=rows(sql,args); if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"NOT_FOUND"); return rows.getFirst(); }
    public Map<String,Object> owned(String table,String id,String user,boolean lock) {
        if(!OWNED.contains(table)) throw new IllegalArgumentException("Unknown resource");
        UUID.fromString(id);
        return one("select * from coach_"+table+" where id=?::uuid and owner=?"+(lock?" for update":""),id,user);
    }
    public void lockUser(String user) { one("select username from coach_users where username=? for update",user); }
    public int update(String sql,Object...args) {return jdbc.update(sql,args);}
}
