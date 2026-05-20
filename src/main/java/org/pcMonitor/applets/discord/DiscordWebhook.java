package org.pcMonitor.applets.discord;

import java.io.*;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/**
 * Class used to execute Discord Webhooks with low effort
 * Adapted from: https://gist.github.com/k3kdude/fba6f6b37594eae3d6f9475330733bdb
 */
public class DiscordWebhook {

    private final String url;
    private final List<EmbedObject> embeds = new ArrayList<>();
    private String content;
    private String username;
    private String avatarUrl;
    private boolean tts;
    private File file;

    public DiscordWebhook(String url) {
        this.url = url;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public void setTts(boolean tts) {
        this.tts = tts;
    }

    public void addEmbed(EmbedObject embed) {
        this.embeds.add(embed);
    }

    public void setFile(File file) {
        this.file = file;
    }

    public void execute() throws IOException {
        JSONObject json = new JSONObject();

        json.put("content", content);
        json.put("username", username);
        json.put("avatar_url", avatarUrl);
        json.put("tts", tts);

        if (!embeds.isEmpty()) {
            JSONArray embedArray = new JSONArray();
            for (EmbedObject embed : embeds) {
                JSONObject jsonEmbed = new JSONObject();

                jsonEmbed.put("title", embed.getTitle());
                jsonEmbed.put("description", embed.getDescription());
                jsonEmbed.put("url", embed.getUrl());

                if (embed.getColor() != 0) {
                    jsonEmbed.put("color", embed.getColor());
                }

                EmbedObject.Footer footer = embed.getFooter();
                if (footer != null) {
                    JSONObject jsonFooter = new JSONObject();
                    jsonFooter.put("text", footer.getText());
                    jsonFooter.put("icon_url", footer.getIconUrl());
                    jsonEmbed.put("footer", jsonFooter);
                }

                EmbedObject.Image image = embed.getImage();
                if (image != null) {
                    JSONObject jsonImage = new JSONObject();
                    jsonImage.put("url", image.getUrl());
                    jsonEmbed.put("image", jsonImage);
                }

                EmbedObject.Thumbnail thumbnail = embed.getThumbnail();
                if (thumbnail != null) {
                    JSONObject jsonThumbnail = new JSONObject();
                    jsonThumbnail.put("url", thumbnail.getUrl());
                    jsonEmbed.put("thumbnail", jsonThumbnail);
                }

                EmbedObject.Author author = embed.getAuthor();
                if (author != null) {
                    JSONObject jsonAuthor = new JSONObject();
                    jsonAuthor.put("name", author.getName());
                    jsonAuthor.put("url", author.getUrl());
                    jsonAuthor.put("icon_url", author.getIconUrl());
                    jsonEmbed.put("author", jsonAuthor);
                }

                List<EmbedObject.Field> fields = embed.getFields();
                if (!fields.isEmpty()) {
                    JSONArray jsonFields = new JSONArray();

                    for (EmbedObject.Field field : fields) {
                        JSONObject jsonField = new JSONObject();
                        jsonField.put("name", field.getName());
                        jsonField.put("value", field.getValue());
                        jsonField.put("inline", field.isInline());

                        jsonFields.add(jsonField);
                    }

                    jsonEmbed.put("fields", jsonFields);
                }

                embedArray.add(jsonEmbed);
            }

            json.put("embeds", embedArray);
        }

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);

        if (file != null) {
            String boundary = "===" + System.currentTimeMillis() + "===";
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (OutputStream outputStream = connection.getOutputStream();
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true)) {

                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"payload_json\"\r\n\r\n");
                writer.append(json.toString()).append("\r\n");

                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                        .append(file.getName()).append("\"\r\n");
                writer.append("Content-Type: application/octet-stream\r\n\r\n").flush();
                try (FileInputStream inputStream = new FileInputStream(file)) {
                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, read);
                    }
                    outputStream.flush();
                }

                writer.append("\r\n").flush();
                writer.append("--").append(boundary).append("--").append("\r\n").flush();
            }
        } else {
            connection.setRequestProperty("Content-Type", "application/json");
            try (OutputStream stream = connection.getOutputStream()) {
                stream.write(json.toString().getBytes());
                stream.flush();
            }
        }

        int responseCode = connection.getResponseCode();
        if (responseCode != 200 && responseCode != 204) {
            InputStream errorStream = connection.getErrorStream();
            String error = new String(errorStream.readAllBytes());
            throw new IOException("Discord webhook failed with code " + responseCode + ": " + error);
        }
    }

    public static class EmbedObject {
        private String title;
        private String description;
        private String url;
        private int color;
        private Footer footer;
        private Thumbnail thumbnail;
        private Image image;
        private Author author;
        private final List<Field> fields = new ArrayList<>();

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String getUrl() {
            return url;
        }

        public int getColor() {
            return color;
        }

        public Footer getFooter() {
            return footer;
        }

        public Thumbnail getThumbnail() {
            return thumbnail;
        }

        public Image getImage() {
            return image;
        }

        public Author getAuthor() {
            return author;
        }

        public List<Field> getFields() {
            return fields;
        }

        public EmbedObject setTitle(String title) {
            this.title = title;
            return this;
        }

        public EmbedObject setDescription(String description) {
            this.description = description;
            return this;
        }

        public EmbedObject setUrl(String url) {
            this.url = url;
            return this;
        }

        public EmbedObject setColor(int color) {
            this.color = color;
            return this;
        }

        public EmbedObject setFooter(String text, String icon) {
            this.footer = new Footer(text, icon);
            return this;
        }

        public EmbedObject setThumbnail(String url) {
            this.thumbnail = new Thumbnail(url);
            return this;
        }

        public EmbedObject setImage(String url) {
            this.image = new Image(url);
            return this;
        }

        public EmbedObject setAuthor(String name, String url, String icon) {
            this.author = new Author(name, url, icon);
            return this;
        }

        public EmbedObject addField(String name, String value, boolean inline) {
            this.fields.add(new Field(name, value, inline));
            return this;
        }

        public static class Footer {
            private final String text;
            private final String iconUrl;

            private Footer(String text, String iconUrl) {
                this.text = text;
                this.iconUrl = iconUrl;
            }

            public String getText() {
                return text;
            }

            public String getIconUrl() {
                return iconUrl;
            }
        }

        public static class Thumbnail {
            private final String url;

            private Thumbnail(String url) {
                this.url = url;
            }

            public String getUrl() {
                return url;
            }
        }

        public static class Image {
            private final String url;

            private Image(String url) {
                this.url = url;
            }

            public String getUrl() {
                return url;
            }
        }

        public static class Author {
            private final String name;
            private final String url;
            private final String iconUrl;

            private Author(String name, String url, String iconUrl) {
                this.name = name;
                this.url = url;
                this.iconUrl = iconUrl;
            }

            public String getName() {
                return name;
            }

            public String getUrl() {
                return url;
            }

            public String getIconUrl() {
                return iconUrl;
            }
        }

        public static class Field {
            private final String name;
            private final String value;
            private final boolean inline;

            private Field(String name, String value, boolean inline) {
                this.name = name;
                this.value = value;
                this.inline = inline;
            }

            public String getName() {
                return name;
            }

            public String getValue() {
                return value;
            }

            public boolean isInline() {
                return inline;
            }
        }
    }

    private static class JSONArray extends ArrayList<Object> {
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("[");
            for (int i = 0; i < size(); i++) {
                Object val = get(i);
                builder.append(val instanceof String ? quote((String) val) : val.toString());
                if (i != size() - 1) builder.append(",");
            }
            builder.append("]");
            return builder.toString();
        }
    }

    private static class JSONObject extends HashMap<String, Object> {
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("{");
            int i = 0;
            for (Map.Entry<String, Object> entry : entrySet()) {
                builder.append(quote(entry.getKey())).append(":");
                Object val = entry.getValue();
                if (val instanceof String) {
                    builder.append(quote((String) val));
                } else if (val instanceof Map || val instanceof JSONArray) {
                    builder.append(val.toString());
                } else if (val != null && val.getClass().isArray()) {
                    builder.append("[");
                    int len = Array.getLength(val);
                    for (int j = 0; j < len; j++) {
                        Object element = Array.get(val, j);
                        if (element instanceof String) {
                            builder.append(quote((String) element));
                        } else {
                            builder.append(element.toString());
                        }
                        if (j != len - 1) builder.append(",");
                    }
                    builder.append("]");
                } else {
                    builder.append(val);
                }
                if (++i < entrySet().size()) builder.append(",");
            }
            builder.append("}");
            return builder.toString();
        }
    }

    private static String quote(String text) {
        return "\"" + text.replace("\"", "\\\"") + "\"";
    }
}
