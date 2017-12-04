import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Andrew Tang
 */
public class DatabaseBuilder {

    private Connection mConnection;
    private final JSONObject mSpecification;
    private final HashMap<String, CreationNode> mCreateJobs;

    public DatabaseBuilder(JSONObject specification, Connection connection) {
        mSpecification = specification;
        mConnection = connection;
        mCreateJobs = new HashMap<>();
    }

    public void addTable(File textFile) {
        try {
            final JSONObject textFileSpec = mSpecification.getJSONObject(textFile.getName());
            final QueryCreator queryCreator = new QueryCreator(textFileSpec, textFile);

            final CreationNode creationNode = new CreationNode(queryCreator);
            mCreateJobs.put(queryCreator.getTableName(), creationNode);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void buildDatabase() {
        Iterator jobs = mCreateJobs.entrySet().iterator();
        while (jobs.hasNext()) {
            Map.Entry entry = (Map.Entry) jobs.next();
            CreationNode node = (CreationNode) entry.getValue();
            node.build();
        }
    }

    /**
     * - tables need to be generated in an order that satisfies foreign key constraints
     * - this means tables that have valeus that reference a column in another table cannot be created until
     *   those dependencies are met.
     * - calling create table on a creation node will check the tables dependencies, retrieve the node from the mBuildJobs
     *   and recursively call create table on that node.
     * Creation Node is a wrapper class for QueryCreator. Where QueryCreator is responsible for
     * Generating the relavent queries for population
     */
    private class CreationNode {
        private final QueryCreator mQueryCreator;
        private final List<String[]> mDependencies;
        private final String mCreateStatement;

        private boolean mBuilt;

        private final String mTableName;
        private long mStartTime;
        private long mFinishTime;

        public CreationNode(QueryCreator queryCreator) {
            mQueryCreator = queryCreator;
            mDependencies = mQueryCreator.getTableDependencies();
            mCreateStatement = mQueryCreator.getCreateQuery();

            mTableName = queryCreator.getTableName();
            mBuilt = false;
        }

        public void build() {
            if (mBuilt) return;

            for (String[] entry : mDependencies) {
                CreationNode node = mCreateJobs.get(entry[1]);
                node.build();
            }

            createTable();
            populateTable();
            mBuilt = true;
        }

        private void createTable() {
            mStartTime = System.currentTimeMillis();

            try {
                mConnection.createStatement().execute(mCreateStatement);
            } catch (SQLException e) {
                e.printStackTrace();
            }

            mFinishTime = System.currentTimeMillis();
            Main.print("Created: " + mTableName);
            Main.print("Time taken: " + new Long((mFinishTime - mStartTime)/1000).toString() + " seconds");
        }

        private void populateTable() {
            Main.print("Populating " + mTableName);
            mStartTime = System.currentTimeMillis();

            mQueryCreator.executeInsertQueries(mConnection);

            mFinishTime = System.currentTimeMillis();
            Main.print("Populated: " + mTableName);
            Main.print("Time taken: " + new Long((mFinishTime - mStartTime)/1000).toString() + " seconds");
        }
    }
}