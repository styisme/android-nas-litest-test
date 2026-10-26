package com.styisme.nasmin;

import android.content.Context;
import android.os.Environment;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import fi.iki.elonen.NanoHTTPD;

public class NasHttpServer extends NanoHTTPD {
    private static final String ROOT = Environment.getExternalStorageDirectory().getAbsolutePath();
    private final Context context;

    public NasHttpServer(int port, Context context) {
        super(port);
        this.context = context;
    }

    private File safeFile(String path) throws IOException {
        if (path == null) path = "/";
        String user = AuthManager.getCurrentUser();
        String userRoot = DatabaseManager.getInstance().getUserRoot(user);
        File rootDir = new File(ROOT, userRoot);
        if (!rootDir.exists()) rootDir.mkdirs();
        String rel = path.replaceAll("^/+", "");
        File f = new File(rootDir, rel);
        String c = f.getCanonicalPath();
        String r = rootDir.getCanonicalPath();
        if (!c.equals(r) && !c.startsWith(r + File.separator)) return null;
        return f;
    }

    private boolean deleteRec(File f) {
        if (!f.exists()) return true;
        if (f.isDirectory()) {
            File[] ch = f.listFiles();
            if (ch != null) for (File x : ch) if (!deleteRec(x)) return false;
        }
        return f.delete();
    }

    @Override public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        try {
            if ("/".equals(uri) || "/index.html".equals(uri)) {
                InputStream is = getClass().getClassLoader().getResourceAsStream("assets/index.html");
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
                return newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8",
                        new String(bos.toByteArray(), "UTF-8"));
            }

            if (!Method.OPTIONS.equals(session.getMethod())) {
                int role = AuthManager.verifyRole(session.getHeaders().get("authorization"));
                if (role == 0) {
                    return text(Response.Status.UNAUTHORIZED, "401");
                }
            }

            if ("/api/status".equals(uri)) {
                return newFixedLengthResponse(Response.Status.OK, "application/json",
                        "{\"running\":true,\"isDefaultPwd\":" + DatabaseManager.isDefaultPassword() + "}");
            }

            if ("/api/list".equals(uri)) {
                String path = session.getParms().get("path");
                if (path == null || path.isEmpty()) path = "/";
                File dir = safeFile(path);
                if (dir == null || !dir.exists() || !dir.isDirectory())
                    return text(Response.Status.NOT_FOUND, "Not found");
                return newFixedLengthResponse(Response.Status.OK, "application/json", buildJson(dir, path));
            }

            if ("/api/download".equals(uri)) {
                File f = safeFile(session.getParms().get("path"));
                if (f == null || !f.exists() || f.isDirectory())
                    return text(Response.Status.NOT_FOUND, "Not found");
                FileInputStream fis = new FileInputStream(f);
                Response r = newFixedLengthResponse(Response.Status.OK, mime(f.getName()), fis, f.length());
                r.addHeader("Content-Disposition", "attachment; filename=\"" + f.getName().replace("\"", "") + "\"");
                return r;
            }

            if ("/api/preview".equals(uri)) {
                File f = safeFile(session.getParms().get("path"));
                if (f == null || !f.exists() || f.isDirectory())
                    return text(Response.Status.NOT_FOUND, "Not found");
                if (f.length() > 2 * 1024 * 1024)
                    return text(Response.Status.FORBIDDEN, "Too large");
                byte[] b = new byte[(int) f.length()];
                FileInputStream fis = new FileInputStream(f);
                int off = 0;
                while (off < b.length) {
                    int n = fis.read(b, off, b.length - off);
                    if (n <= 0) break;
                    off += n;
                }
                fis.close();
                return newFixedLengthResponse(Response.Status.OK, "text/plain; charset=utf-8",
                        new String(b, "UTF-8"));
            }

            if ("/api/search".equals(uri)) {
                String q = session.getParms().get("q");
                if (q == null || q.trim().isEmpty())
                    return newFixedLengthResponse(Response.Status.OK, "application/json", "[]");
                File root = safeFile("/");
                if (root == null) return text(Response.Status.FORBIDDEN, "Forbidden");
                List<File> res = new ArrayList<File>();
                searchFiles(root, q.toLowerCase(), res, 0);
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < res.size(); i++) {
                    if (i > 0) sb.append(",");
                    File f = res.get(i);
                    sb.append("{\"name\":\"").append(esc(f.getName()))
                            .append("\",\"path\":\"").append(esc(relPath(root, f)))
                            .append("\",\"dir\":").append(f.isDirectory())
                            .append(",\"size\":").append(f.length()).append("}");
                }
                sb.append("]");
                return newFixedLengthResponse(Response.Status.OK, "application/json", sb.toString());
            }

            if (Method.PUT.equals(session.getMethod()) && "/api/upload".equals(uri)) {
                String path = session.getParms().get("path");
                String name = session.getParms().get("name");
                if (name == null) return text(Response.Status.BAD_REQUEST, "Missing name");
                File dir = safeFile(path);
                if (dir == null) return text(Response.Status.FORBIDDEN, "Forbidden");
                if (!dir.exists()) dir.mkdirs();
                File target = new File(dir, new File(name).getName());
                long len = 0;
                try { len = Long.parseLong(session.getHeaders().get("content-length")); } catch (Exception ignored) {}
                FileOutputStream fos = new FileOutputStream(target);
                InputStream is = session.getInputStream();
                byte[] buf = new byte[8192];
                long total = 0;
                while (total < len) {
                    int toRead = (int) Math.min(buf.length, len - total);
                    int n = is.read(buf, 0, toRead);
                    if (n <= 0) break;
                    fos.write(buf, 0, n);
                    total += n;
                }
                fos.close();
                return text(Response.Status.OK, "OK");
            }

            if ("/api/save".equals(uri) && Method.POST.equals(session.getMethod())) {
                Map<String, String> files = new HashMap<String, String>();
                session.parseBody(files);
                String path = session.getParms().get("path");
                String content = session.getParms().get("content");
                File f = safeFile(path);
                if (f == null) return text(Response.Status.FORBIDDEN, "Forbidden");
                FileOutputStream fos = new FileOutputStream(f);
                fos.write(content == null ? new byte[0] : content.getBytes("UTF-8"));
                fos.close();
                return text(Response.Status.OK, "OK");
            }

            if ("/api/mkdir".equals(uri) && Method.POST.equals(session.getMethod())) {
                Map<String, String> files = new HashMap<String, String>();
                session.parseBody(files);
                String path = session.getParms().get("path");
                String name = session.getParms().get("name");
                File dir = safeFile(path);
                if (dir == null || name == null) return text(Response.Status.FORBIDDEN, "Forbidden");
                File target = new File(dir, new File(name).getName());
                return text(Response.Status.OK, target.mkdirs() ? "OK" : "Fail");
            }

            if ("/api/rename".equals(uri) && Method.POST.equals(session.getMethod())) {
                Map<String, String> files = new HashMap<String, String>();
                session.parseBody(files);
                String path = session.getParms().get("path");
                String newName = session.getParms().get("newName");
                File f = safeFile(path);
                if (f == null || newName == null) return text(Response.Status.FORBIDDEN, "Forbidden");
                File target = new File(f.getParentFile(), new File(newName).getName());
                return text(Response.Status.OK, f.renameTo(target) ? "OK" : "Fail");
            }

            if ("/api/delete".equals(uri) && Method.POST.equals(session.getMethod())) {
                Map<String, String> files = new HashMap<String, String>();
                session.parseBody(files);
                File f = safeFile(session.getParms().get("path"));
                if (f == null) return text(Response.Status.FORBIDDEN, "Forbidden");
                return text(Response.Status.OK, deleteRec(f) ? "OK" : "Fail");
            }

            if ("/api/unzip".equals(uri) && Method.POST.equals(session.getMethod())) {
                Map<String, String> files = new HashMap<String, String>();
                session.parseBody(files);
                File f = safeFile(session.getParms().get("path"));
                if (f == null || !f.exists()) return text(Response.Status.FORBIDDEN, "Forbidden");
                return text(Response.Status.OK, unzip(f) ? "OK" : "Fail");
            }

            if ("/api/password".equals(uri) && Method.POST.equals(session.getMethod())) {
                Map<String, String> files = new HashMap<String, String>();
                session.parseBody(files);
                String pwd = session.getParms().get("password");
                if (pwd == null || pwd.length() < 4) return text(Response.Status.BAD_REQUEST, "Too short");
                DatabaseManager.getInstance().changePassword("admin", pwd);
                return text(Response.Status.OK, "OK");
            }

        } catch (Exception e) {
            return text(Response.Status.INTERNAL_ERROR, "Error: " + e.getMessage());
        } finally {
            AuthManager.clearCurrentUser();
        }
        return text(Response.Status.NOT_FOUND, "Not found");
    }

    private void searchFiles(File dir, String q, List<File> res, int depth) {
        if (dir == null || !dir.exists() || depth > 4 || res.size() >= 200) return;
        File[] fs = dir.listFiles();
        if (fs == null) return;
        for (File f : fs) {
            if (f.getName().toLowerCase().contains(q)) res.add(f);
            if (f.isDirectory() && !f.getName().startsWith(".")) searchFiles(f, q, res, depth + 1);
        }
    }

    private String relPath(File root, File f) {
        try {
            String r = root.getCanonicalPath();
            String c = f.getCanonicalPath();
            if (c.equals(r)) return "/";
            if (c.startsWith(r + File.separator)) return c.substring(r.length()).replace('\\', '/');
        } catch (Exception ignored) {}
        return "/";
    }

    private boolean unzip(File zip) {
        try {
            ZipInputStream zis = new ZipInputStream(new FileInputStream(zip));
            ZipEntry e;
            File dest = zip.getParentFile();
            String destCanon = dest.getCanonicalPath();
            byte[] buf = new byte[8192];
            while ((e = zis.getNextEntry()) != null) {
                File f = new File(dest, e.getName());
                String fc = f.getCanonicalPath();
                if (!fc.equals(destCanon) && !fc.startsWith(destCanon + File.separator)) continue;
                if (e.isDirectory()) f.mkdirs();
                else {
                    File p = f.getParentFile();
                    if (p != null) p.mkdirs();
                    FileOutputStream fos = new FileOutputStream(f);
                    int n;
                    while ((n = zis.read(buf)) > 0) fos.write(buf, 0, n);
                    fos.close();
                }
            }
            zis.close();
            return true;
        } catch (Exception e) { return false; }
    }

    private String mime(String n) {
        n = n.toLowerCase();
        if (n.endsWith(".mp4")) return "video/mp4";
        if (n.endsWith(".mp3")) return "audio/mpeg";
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return "image/jpeg";
        if (n.endsWith(".png")) return "image/png";
        if (n.endsWith(".gif")) return "image/gif";
        if (n.endsWith(".txt")) return "text/plain; charset=utf-8";
        if (n.endsWith(".html")) return "text/html; charset=utf-8";
        return "application/octet-stream";
    }

    private String buildJson(File dir, String path) {
        File[] files = dir.listFiles();
        if (files == null) files = new File[0];
        Arrays.sort(files, new Comparator<File>() {
            @Override public int compare(File a, File b) {
                if (a.isDirectory() && !b.isDirectory()) return -1;
                if (!a.isDirectory() && b.isDirectory()) return 1;
                return a.getName().compareToIgnoreCase(b.getName());
            }
        });
        StringBuilder sb = new StringBuilder("{\"path\":\"" + esc(path) + "\",\"files\":[");
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if (i > 0) sb.append(",");
            sb.append("{\"name\":\"").append(esc(f.getName()))
                    .append("\",\"dir\":").append(f.isDirectory())
                    .append(",\"size\":").append(f.length()).append("}");
        }
        sb.append("]}");
        return sb.toString();
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private Response text(Response.Status s, String m) {
        return newFixedLengthResponse(s, "text/plain; charset=utf-8", m);
    }
}
