import com.primavera.PrimaveraException;
import com.primavera.integration.client.Session;
import com.primavera.integration.client.EnterpriseLoadManager;
import com.primavera.integration.client.RMIURL;
import com.primavera.integration.client.bo.object.*;
import com.primavera.integration.common.DatabaseInstance;
import com.primavera.integration.client.bo.BOIterator;

import java.io.IOException;

public class TestIntegrationAPI {
    private static final String PRIMAVERA_BOOTSTRAP_HOME_KEY = "primavera.bootstrap.home";
    private static final String PRIMAVERA_BOOTSTRAP_HOME_VALUE = "C:\\P6IntegrationAPI_1"; //mb env?

    private static final int DB_NAME_ARGS_INDEX = 0;
    private static final int DB_LOGIN_ARGS_INDEX = 1;
    private static final int DB_PASSWORD_ARGS_INDEX = 2;
    private static final int DB_PROJECT_IDS_ARGS_INDEX = 3;

    //args = "PMDB" "Admin" "admin" "EC00610,EC02016" *aka* DBInstance Login Pass ProjectIDs
    public static void main(String[] args) throws IOException, PrimaveraException {
        System.setProperty(PRIMAVERA_BOOTSTRAP_HOME_KEY, PRIMAVERA_BOOTSTRAP_HOME_VALUE);
        Session session = null;

        try {
            if (args.length != 4) throw new IOException("Invalid program args length..");
            String dbName = args[DB_NAME_ARGS_INDEX];
            String dbLogin = args[DB_LOGIN_ARGS_INDEX];
            String dbPass = args[DB_PASSWORD_ARGS_INDEX];
            String dbProjectIDs = args[DB_PROJECT_IDS_ARGS_INDEX];
            String dbId = null;

            DatabaseInstance[] dbInstances = Session.getDatabaseInstances(RMIURL.getRmiUrl(RMIURL.LOCAL_SERVICE));
            for (DatabaseInstance dbi : dbInstances) {
                if (dbName.equalsIgnoreCase(dbi.getDatabaseName())) {
                    dbId = dbi.getDatabaseId();
                    session = Session.login(RMIURL.getRmiUrl(RMIURL.LOCAL_SERVICE), dbId, dbLogin, dbPass);
                    break;
                }
            }
            if (dbId == null) throw new PrimaveraException("DB instance '" + dbName + "' not found..");

            String whereClause = generateWhereClause(dbProjectIDs);
            EnterpriseLoadManager elm = session.getEnterpriseLoadManager();
            BOIterator<Project> boi = elm.loadProjects(new String[]{"Id", "Name"}, whereClause, "Name asc");
            if (!boi.hasNext()) throw new PrimaveraException("Projects not found..");

            while (boi.hasNext()) {
                Project project = boi.next();

                BOIterator<Activity> boiActivities =
                        project.loadAllActivities(new String[]{"Id", "Name"},
                                null,
                                "Name asc");
                while (boiActivities.hasNext()) {
                    Activity activity = boiActivities.next();
                    System.out.println("activity.getName()= " + activity.getName());

                    BOIterator<ResourceAssignment> boiResourceAssignments =
                            activity.loadResourceAssignments(new String[]{"ResourceType", "RemainingUnits"},
                                    "ResourceType = 'Labor'", //"Material" is not exist in my sampleDB - "Labor" have 'RemainingUnits' && hardcode_^
                                    null);
                    while (boiResourceAssignments.hasNext()) {
                        ResourceAssignment resourceAssignment = boiResourceAssignments.next();
                        System.out.println("resourceAssignment.getResourceType()= " + resourceAssignment.getResourceType());
                        System.out.println("resourceAssignment.getRemainingUnits()= " + resourceAssignment.getRemainingUnits()); //Unit<-PrmNumber<-Number
                    }
                }

                BOIterator<ActivityCodeAssignment> boiActivityCodeAssignment =
                        project.loadAllActivityCodeAssignments(new String[]{"ActivityCodeTypeName"},
                                null,
                                null);
                while (boiActivityCodeAssignment.hasNext()) {
                    ActivityCodeAssignment activityCodeAssignment = boiActivityCodeAssignment.next();
                    System.out.println("activityCodeAssignment.getActivityCodeTypeName()= " + activityCodeAssignment.getActivityCodeTypeName());

                    ActivityCodeType activityCodeType = activityCodeAssignment.loadActivityCodeType(new String[]{"Name"});
                    System.out.println("activityCodeType.getName()= " + activityCodeType.getName());
                }

                System.out.println(project.getName());
                //Have no idea what could be here..
            }
        } catch (IOException | PrimaveraException e) {
            e.printStackTrace();
        } finally {
            if (session != null) session.logout();
        }
    }

    private static String generateWhereClause(String projectIDsFromArgs) {
        String[] projectIDsArray = projectIDsFromArgs.split(",");
        StringBuffer sb = new StringBuffer();
        int i = 0;

        for (String id : projectIDsArray) {
            if (i == 0) {
                sb.append("Id = '").append(id.trim()).append("'");
            } else {
                sb.append(" OR ").append("Id = '").append(id.trim()).append("'");
            }
            i++;
        }

        return sb.toString();
    }
}
