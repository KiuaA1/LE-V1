package net.kdt.pojavlaunch.value;

import androidx.annotation.Keep;
import net.kdt.pojavlaunch.JMinecraftVersionList.Arguments.ArgValue.ArgRules;

@Keep
public class DependentLibrary {
    public ArgRules[] rules;
    public String name;
    public LibraryDownloads downloads;
    public Extract extract;
    public java.util.Map<String, String> natives; // OS -> classifier, e.g. "linux" -> "natives-linux"
    public String url;

    @Keep
	public static class LibraryDownloads {
		public MinecraftLibraryArtifact artifact;
        public java.util.Map<String, MinecraftLibraryArtifact> classifiers;
		public LibraryDownloads(MinecraftLibraryArtifact artifact) {
			this.artifact = artifact;
		}
	}
}

