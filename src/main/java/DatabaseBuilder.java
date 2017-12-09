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
            Main.print(e.getMessage());
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

    private class CreationNode {
        private final QueryCreator mQueryCreator;
        private final List<String[]> mDependencies;
        private final String mCreateStatement;
        private final List<String> mIndexQueries;

        private boolean mBuilt;

        private final String mTableName;
        private double mStartTime;
        private double mFinishTime;

        public CreationNode(QueryCreator queryCreator) {
            mQueryCreator = queryCreator;
            mDependencies = mQueryCreator.getTableDependencies();
            mCreateStatement = mQueryCreator.getCreateQuery();
            mIndexQueries = mQueryCreator.getIndexQueries();

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
            createIndices();
            try {
                mConnection.commit();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            mBuilt = true;

            Main.print("==============================");
        }

        private void createTable() {
            Main.print("Creating " + mTableName);
            try {
                mConnection.createStatement().execute(mCreateStatement);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        private void populateTable() {
            Main.print("Populating...");
            mStartTime = System.currentTimeMillis();

            mQueryCreator.executeInsertQueries(mConnection);

            mFinishTime = System.currentTimeMillis();
            printTimeTaken();
        }

        private void createIndices() {
            if (mIndexQueries.isEmpty()) {
                return;
            }
            Main.print("Creating indices...");
            mStartTime = System.currentTimeMillis();

            for (String query : mIndexQueries) {
                try {
                    mConnection.createStatement().execute(query);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            mFinishTime = System.currentTimeMillis();
            printTimeTaken();
        }

        private void printTimeTaken() {
            double  timeInS = (mFinishTime - mStartTime)/1000;
            double  timeInMs = (mFinishTime - mStartTime);

            double time = timeInS > 1 ? timeInS : timeInMs;
            String unit = timeInS > 1 ? " seconds" : "ms";

            Main.print("Done. Time taken: " + time + unit);
        }
    }
}