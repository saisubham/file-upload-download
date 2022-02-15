package com.sudosai.fileupload;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StorageServiceImpl implements StorageService {

	private final Path rootPath = Paths.get("upload");

	@Override
	public void init() {
		try {
			Files.createDirectories(rootPath);
		} catch (Exception e) {
			throw new RuntimeException("Could not create root directory", e);
		}
	}

	@Override
	public void store(MultipartFile file) {
		try {
			if (file.isEmpty()) {
				throw new RuntimeException("Cannot store empty file");
			}
			Path destPath = this.rootPath.resolve(Paths.get(file.getOriginalFilename())).normalize().toAbsolutePath();
			if (!destPath.getParent().equals(this.rootPath.toAbsolutePath())) {
				throw new RuntimeException("Cannot store file outside of current directory");
			}
			try (InputStream in = file.getInputStream()) {
				Files.copy(in, destPath, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to store file", e);
		}
	}

	@Override
	public Stream<Path> loadAll() {
		try {
			return Files.walk(this.rootPath, 1).filter(path -> !path.equals(this.rootPath))
					.map(this.rootPath::relativize);
		} catch (Exception e) {
			throw new RuntimeException("Failed to read stored files", e);
		}
	}

	@Override
	public Path load(String filename) {
		return rootPath.resolve(filename);
	}

	@Override
	public Resource loadAsResource(String filename) {
		try {
			Path path = load(filename);
			Resource resource = new UrlResource(path.toUri());
			if (resource.exists() || resource.isReadable()) {
				return resource;
			}
			throw new RuntimeException("Could not read file " + filename);
		} catch (Exception e) {
			throw new RuntimeException("Could not read file " + filename, e);
		}
	}

	@Override
	public void deleteAll() {
		FileSystemUtils.deleteRecursively(rootPath.toFile());
	}
}
