package land.face.strife.storage;

import com.tealcube.minecraft.bukkit.facecore.database.Database;
import com.tealcube.minecraft.bukkit.facecore.database.MySqlDatabasePool;
import java.util.UUID;
import land.face.strife.StrifePlugin;
import land.face.strife.data.champion.ChampionSaveData;

public class MySqlDataStorage implements DataStorage {

    private final StrifePlugin plugin;
    private Database database;

    public MySqlDataStorage(StrifePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() {
        if (database != null) {
            // do nothing
            return;
        }
        this.database = new MySqlDatabasePool(
                plugin.getSettings().getString("config.storage.host", "localhost"),
                plugin.getSettings().getString("config.storage.port", "3306"),
                plugin.getSettings().getString("config.storage.database", "localdb"),
                plugin.getSettings().getString("config.storage.user", "localuser"),
                plugin.getSettings().getString("config.storage.pass", "localpass"));
    }

    @Override
    public void shutdown() {

    }

    @Override
    public void saveAll() {

    }

    @Override
    public void save(ChampionSaveData championSaveData) {

    }

    @Override
    public ChampionSaveData load(UUID uuid) {
        return null;
    }

}
