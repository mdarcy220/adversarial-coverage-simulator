package adsim;

public interface SettingsReloadable {
	/**
	 * Reloads the internal cached settings for this object based on the global
	 * settings for the simulation.
	 */
	public void reloadSettings();
}
