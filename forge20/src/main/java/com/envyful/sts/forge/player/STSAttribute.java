package com.envyful.sts.forge.player;

import com.envyful.api.forge.player.ForgePlayerManager;
import com.envyful.api.forge.player.attribute.ManagedForgeAttribute;
import com.envyful.api.player.save.attribute.DataDirectory;
import com.envyful.api.time.UtilTimeFormat;
import com.envyful.sts.forge.EnvySTSForge;
import com.envyful.sts.forge.config.STSConfig;
import com.envyful.sts.forge.config.STSQueries;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

@DataDirectory("config/players/EnvySTS/")
public class STSAttribute extends ManagedForgeAttribute<EnvySTSForge> {

    private transient int selectedSlot = -1;

    private long lastUse = -1L;

    public STSAttribute(ForgePlayerManager playerManager) {
        super(EnvySTSForge.getInstance(), playerManager);
    }

    public int getSelectedSlot() {
        return this.selectedSlot;
    }

    public void setSelectedSlot(int selectedSlot) {
        this.selectedSlot = selectedSlot;
    }

    public void setLastUse(long lastUse) {
        this.lastUse = lastUse;
    }

    public boolean onCooldown() {
        STSConfig.Cooldown cooldown = EnvySTSForge.getConfig().getCooldown(this.parent.getParent());

        if (cooldown == null || this.lastUse == -1L) {
            return false;
        }

        return (System.currentTimeMillis() - this.lastUse) <= TimeUnit.SECONDS.toMillis(cooldown.getDurationSeconds());
    }

    public String getRemainingTime() {
        STSConfig.Cooldown cooldown = EnvySTSForge.getConfig().getCooldown(this.parent.getParent());
        return UtilTimeFormat.getFormattedDuration(TimeUnit.SECONDS.toMillis(cooldown.getDurationSeconds() -
                TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - this.lastUse)));
    }

    @Override
    public void load() {
        try (Connection connection = EnvySTSForge.getDatabase().getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(STSQueries.LOAD_DATA)) {
            preparedStatement.setString(1, this.parent.getUuid().toString());

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    this.lastUse = resultSet.getLong("last_use");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void save() {
        try (Connection connection = EnvySTSForge.getDatabase().getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(STSQueries.UPDATE_DATA)) {
            preparedStatement.setString(1, this.parent.getUuid().toString());
            preparedStatement.setLong(2, this.lastUse);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
