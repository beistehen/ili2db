package ch.ehi.ili2ora;

import java.sql.Connection;

import ch.ehi.basics.logging.EhiLogger;
import ch.ehi.ili2db.base.DbIdGen;
import ch.ehi.ili2db.gui.Config;
import ch.ehi.sqlgen.generator.Generator;
import ch.ehi.sqlgen.generator_impl.jdbc.GeneratorJdbc;
import ch.ehi.sqlgen.repository.DbSchema;
import ch.ehi.sqlgen.repository.DbTableName;

public class OraSequenceBasedIdGen implements DbIdGen {

	public static final String SQL_ILI2DB_SEQ_NAME="t_ili2db_seq";
	java.sql.Connection conn=null;
	String dbusr=null;
	String schema=null;
	Long minValue=null;
	Long maxValue=null;
	
	Long startWith;
	
	@Override
	public void init(String schema, Config config) {
		this.schema=schema;
		minValue=config.getMinIdSeqValue();
		maxValue=config.getMaxIdSeqValue();
		
		startWith = 1L;
	}

	@Override
	public void initDb(Connection conn, String dbusr) {
		this.conn=conn;
		this.dbusr=dbusr;
	}

	@Override
	public void initDbDefs(Generator gen) {
		DbTableName sqlName=new DbTableName(schema,SQL_ILI2DB_SEQ_NAME);
		String stmt="CREATE SEQUENCE "+sqlName.getQName()+ " START WITH "+startWith;
		if(minValue!=null){
			stmt=stmt+" MINVALUE "+minValue;
		}
		if(maxValue!=null){
			stmt=stmt+" MAXVALUE "+maxValue;
		}

		if(gen instanceof GeneratorJdbc){
			((GeneratorJdbc) gen).addCreateLine(((GeneratorJdbc) gen).new Stmt(stmt));
			((GeneratorJdbc) gen).addDropLine(((GeneratorJdbc) gen).new Stmt("DROP SEQUENCE "+sqlName.getQName()));
		}
        if(conn!=null) {
            EhiLogger.traceBackendCmd(stmt);
            java.sql.PreparedStatement updstmt = null;
            try{
                try {
                    updstmt = conn.prepareStatement(stmt);
                    updstmt.execute();
                } finally {
                    if(updstmt!=null) updstmt.close();
                }
            }catch(java.sql.SQLException ex){
                EhiLogger.logError("Failed to create sequence "+sqlName.getQName(),ex);
            }
        }
	}
	
	long lastLocalId=0;
	@Override
	public long newObjSqlId(){
		lastLocalId=getSeqCount();
		return lastLocalId;
	}
	@Override
	public long getLastSqlId()
	{
		return lastLocalId;
	}
	private long getSeqCount()
	{
		String sqlName=SQL_ILI2DB_SEQ_NAME;
		if(schema!=null){
			sqlName=schema+"."+sqlName;
		}
        try{
            java.sql.PreparedStatement getstmt=null;
            java.sql.ResultSet res=null;
            try {
                String stmt="select "+sqlName+".nextval from dual";
                EhiLogger.traceBackendCmd(stmt);
                getstmt=conn.prepareStatement(stmt);
                res=getstmt.executeQuery();
                long ret=0;
                if(res.next()){
                    ret=res.getLong(1);
                    return ret;
                }
            } finally {
                if(getstmt!=null) {
                    if(res!=null) res.close();
                    getstmt.close();
                }
            }
        }catch(java.sql.SQLException ex){
            EhiLogger.logError("Failed to query "+sqlName,ex);
            throw new IllegalStateException(ex);
        }
		throw new IllegalStateException("no nextval "+sqlName);
	}
	@Override
	public String getDefaultValueSql() {
		String sqlName=SQL_ILI2DB_SEQ_NAME;
		if(schema!=null){
			sqlName=schema+"."+sqlName;
		}
		return sqlName+".nextval ";
	}

	@Override
	public void addMappingTable(DbSchema schema) {
        // not implemented because it's based on Oracle sequences
	}
}
