package com.manning.writeflow.outboxrelay;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.h2.api.Trigger;

/** Fails the first outbox marking update to exercise retry behavior.
 *
 * @author Enrique Medina Montenegro (emedina)
 */
public final class FailFirstMarkTrigger implements Trigger {

    private static boolean failed;

    @Override
    public void init(final Connection connection, final String schemaName, final String triggerName,
            final String tableName, final boolean before, final int type) {
        failed = false;
    }

    @Override
    public void fire(final Connection connection, final Object[] oldRow, final Object[] newRow) throws SQLException {
        if (!failed) {
            failed = true;
            throw new SQLException("programmed mark failure");
        }
    }

    @Override
    public void close() {
    }

    @Override
    public void remove() {
    }
}
