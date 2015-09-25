package eu.smartsantander.androidExperimentation.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: theodori
 * Date: 9/4/13
 * Time: 11:06 AM
 * To change this template use File | Settings | File Templates.
 */
@Entity
public class Plugin  implements Serializable {
    @Id
    @GeneratedValue
    private int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private String contextType;

    public String getContextType() {
        return contextType;
    }

    public void setContextType(String contextType) {
        this.contextType = contextType;
    }

    private String runtimeFactoryClass;

    public String getRuntimeFactoryClass() {
        return runtimeFactoryClass;
    }

    public void setRuntimeFactoryClass(String runtimeFactoryClass) {
        this.runtimeFactoryClass = runtimeFactoryClass;
    }

    private String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    private String installUrl;

    public String getInstallUrl() {
        return installUrl;
    }

    public void setInstallUrl(String installUrl) {
        this.installUrl = installUrl;
    }

    private String filename;

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Plugin that = (Plugin) o;

        if (id != that.id) return false;
        if (contextType != null ? !contextType.equals(that.contextType) : that.contextType != null) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (filename != null ? !filename.equals(that.filename) : that.filename != null) return false;
        if (installUrl != null ? !installUrl.equals(that.installUrl) : that.installUrl != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (runtimeFactoryClass != null ? !runtimeFactoryClass.equals(that.runtimeFactoryClass) : that.runtimeFactoryClass != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (contextType != null ? contextType.hashCode() : 0);
        result = 31 * result + (runtimeFactoryClass != null ? runtimeFactoryClass.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (installUrl != null ? installUrl.hashCode() : 0);
        result = 31 * result + (filename != null ? filename.hashCode() : 0);
        return result;
    }
}
