package com.zeroffa.tdbinpacking.visualization;

import com.zeroffa.tdbinpacking.application.PackingRunResult;
import com.zeroffa.tdbinpacking.model.ContainerBox;
import com.zeroffa.tdbinpacking.model.PackingResult;
import com.zeroffa.tdbinpacking.model.Placement;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class PackingResultVisualizer {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    public Path exportAsHtml(PackingResult packingResult, Path outputPath) throws IOException {
        return writeHtml(outputPath, buildHtml(packingResult, "single-run", Instant.now(), 0.0D));
    }

    public Path exportAsHtml(PackingRunResult runResult, Path outputPath) throws IOException {
        return writeHtml(
                outputPath,
                buildHtml(
                        runResult.getPackingResult(),
                        runResult.getPackingCase().getCaseId(),
                        runResult.getStartedAt(),
                        runResult.getDurationMillis()
                )
        );
    }

    private Path writeHtml(Path outputPath, String html) throws IOException {
        Path parent = outputPath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(outputPath, html, StandardCharsets.UTF_8);
        return outputPath.toAbsolutePath();
    }

    private String buildHtml(PackingResult packingResult, String caseId, Instant startedAtInstant, double runtimeMillis) {
        StringBuilder dataBuilder = new StringBuilder();
        dataBuilder.append("[");
        for (int index = 0; index < packingResult.getPlacements().size(); index++) {
            Placement placement = packingResult.getPlacements().get(index);
            if (index > 0) {
                dataBuilder.append(",");
            }
            dataBuilder.append("{")
                    .append("\"id\":\"").append(escapeJs(placement.getItem().getId())).append("\",")
                    .append("\"x\":").append(placement.getX()).append(",")
                    .append("\"y\":").append(placement.getY()).append(",")
                    .append("\"z\":").append(placement.getZ()).append(",")
                    .append("\"sizeX\":").append(placement.getSizeX()).append(",")
                    .append("\"sizeY\":").append(placement.getSizeY()).append(",")
                    .append("\"sizeZ\":").append(placement.getSizeZ())
                    .append("}");
        }
        dataBuilder.append("]");

        ContainerBox container = packingResult.getContainerBox();
        String summary = "Packed: " + packingResult.getPackedVolume()
                + " / " + packingResult.getContainerVolume()
                + " (" + String.format("%.2f%%", packingResult.getUtilization() * 100.0D) + ")";
        String runTime = String.format("%.3f ms", runtimeMillis);
        String startedAt = TIME_FORMATTER.format(startedAtInstant);

        return """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  <title>3D Bin Packing Visualization</title>
                  <style>
                    html, body {
                      margin: 0;
                      height: 100%%;
                      font-family: "Segoe UI", Tahoma, sans-serif;
                      background: #f4f5f6;
                    }
                    #canvas-wrap {
                      position: fixed;
                      inset: 0;
                    }
                    #legend {
                      position: fixed;
                      top: 16px;
                      right: 16px;
                      width: 300px;
                      max-height: calc(100%% - 32px);
                      overflow: auto;
                      background: rgba(255, 255, 255, 0.95);
                      border: 1px solid #d6d8db;
                      border-radius: 10px;
                      padding: 12px;
                      box-shadow: 0 8px 20px rgba(0, 0, 0, 0.10);
                      font-size: 13px;
                      line-height: 1.4;
                    }
                    #legend h2 {
                      margin: 0 0 8px;
                      font-size: 15px;
                    }
                    #legend ul {
                      margin: 8px 0 0;
                      padding-left: 18px;
                    }
                    #legend li {
                      margin: 3px 0;
                    }
                    #tips {
                      position: fixed;
                      left: 16px;
                      bottom: 16px;
                      background: rgba(0, 0, 0, 0.65);
                      color: #fff;
                      padding: 8px 10px;
                      border-radius: 8px;
                      font-size: 12px;
                    }
                    .swatch {
                      display: inline-block;
                      width: 10px;
                      height: 10px;
                      margin-right: 6px;
                      border: 1px solid rgba(0, 0, 0, 0.25);
                      vertical-align: middle;
                    }
                  </style>
                </head>
                <body>
                  <div id="canvas-wrap"></div>
                  <aside id="legend">
                    <h2>3D Bin Packing</h2>
                    <div>Case: %s</div>
                    <div>Started At: %s</div>
                    <div>Runtime: %s</div>
                    <div>%s</div>
                    <div>Container: [%d, %d, %d]</div>
                    <ul id="items"></ul>
                  </aside>
                  <div id="tips">Mouse: rotate | wheel: zoom | right drag: pan</div>
                
                  <script src="https://cdn.jsdelivr.net/npm/three@0.128.0/build/three.min.js"></script>
                  <script src="https://cdn.jsdelivr.net/npm/three@0.128.0/examples/js/controls/OrbitControls.js"></script>
                  <script src="https://cdn.jsdelivr.net/npm/three@0.128.0/examples/js/lines/LineSegmentsGeometry.js"></script>
                  <script src="https://cdn.jsdelivr.net/npm/three@0.128.0/examples/js/lines/LineGeometry.js"></script>
                  <script src="https://cdn.jsdelivr.net/npm/three@0.128.0/examples/js/lines/LineMaterial.js"></script>
                  <script src="https://cdn.jsdelivr.net/npm/three@0.128.0/examples/js/lines/LineSegments2.js"></script>
                  <script src="https://cdn.jsdelivr.net/npm/three@0.128.0/examples/js/lines/Line2.js"></script>
                  <script>
                    const tips = document.getElementById('tips');

                    function hasWebGLSupport() {
                      try {
                        const canvas = document.createElement('canvas');
                        return !!(canvas.getContext('webgl') || canvas.getContext('experimental-webgl'));
                      } catch (error) {
                        return false;
                      }
                    }

                    try {
                      if (!window.THREE || !THREE.OrbitControls || !THREE.LineSegments2
                          || !THREE.LineSegmentsGeometry || !THREE.LineMaterial) {
                        throw new Error('Failed to load Three.js from CDN.');
                      }
                      if (!hasWebGLSupport()) {
                        throw new Error('WebGL is not available in this browser or environment.');
                      }

                      const container = { sizeX: %d, sizeY: %d, sizeZ: %d };
                      const placements = %s;

                      const root = document.getElementById('canvas-wrap');
                      const scene = new THREE.Scene();
                      scene.background = new THREE.Color(0xf4f5f6);
                      scene.up.set(0, 0, 1);

                      const maxDim = Math.max(container.sizeX, container.sizeY, container.sizeZ);
                      const camera = new THREE.PerspectiveCamera(45, window.innerWidth / window.innerHeight, 0.1, 10000);
                      camera.up.set(0, 0, 1);
                      camera.position.set(maxDim * 1.8, maxDim * 1.6, maxDim * 1.6);

                      const renderer = new THREE.WebGLRenderer({ antialias: true });
                      renderer.setPixelRatio(window.devicePixelRatio);
                      renderer.setSize(window.innerWidth, window.innerHeight);
                      root.appendChild(renderer.domElement);

                      const resizableLineMaterials = [];

                      const controls = new THREE.OrbitControls(camera, renderer.domElement);
                      controls.target.set(0, 0, 0);
                      controls.update();

                      const ambient = new THREE.AmbientLight(0xffffff, 0.65);
                      scene.add(ambient);
                      const directional = new THREE.DirectionalLight(0xffffff, 0.7);
                      directional.position.set(1, 1, 2);
                      scene.add(directional);

                      const halfX = container.sizeX / 2;
                      const halfY = container.sizeY / 2;
                      const halfZ = container.sizeZ / 2;

                      const containerEdges = new THREE.EdgesGeometry(
                        new THREE.BoxGeometry(container.sizeX, container.sizeY, container.sizeZ)
                      );
                      const containerLine = new THREE.LineSegments(
                        containerEdges,
                        new THREE.LineBasicMaterial({ color: 0x1f2937 })
                      );
                      scene.add(containerLine);

                      function addContainerFaceGrid(sizeA, sizeB, origin, axisA, axisB, color) {
                        const positions = [];
                        for (let a = 0; a <= sizeA; a += 1) {
                          const start = origin.clone();
                          start[axisA] += a;
                          const end = start.clone();
                          end[axisB] += sizeB;
                          positions.push(start.x, start.y, start.z, end.x, end.y, end.z);
                        }
                        for (let b = 0; b <= sizeB; b += 1) {
                          const start = origin.clone();
                          start[axisB] += b;
                          const end = start.clone();
                          end[axisA] += sizeA;
                          positions.push(start.x, start.y, start.z, end.x, end.y, end.z);
                        }
                        const geometry = new THREE.LineSegmentsGeometry();
                        geometry.setPositions(positions);
                        const material = new THREE.LineMaterial({
                          color: color,
                          transparent: false,
                          opacity: 1,
                          depthWrite: false,
                          depthTest: false,
                          linewidth: 1,
                          resolution: new THREE.Vector2(window.innerWidth, window.innerHeight)
                        });
                        const line = new THREE.LineSegments2(geometry, material);
                        line.computeLineDistances();
                        line.renderOrder = 20;
                        scene.add(line);
                        resizableLineMaterials.push(material);
                      }

                      const gridInset = -2.0;
                      addContainerFaceGrid(
                        container.sizeX,
                        container.sizeY,
                        new THREE.Vector3(-halfX, -halfY, -halfZ - gridInset),
                        'x',
                        'y',
                        0xd8d8d8
                      );
                      addContainerFaceGrid(
                        container.sizeX,
                        container.sizeZ,
                        new THREE.Vector3(-halfX, -halfY - gridInset, -halfZ),
                        'x',
                        'z',
                        0xd8d8d8
                      );
                      addContainerFaceGrid(
                        container.sizeY,
                        container.sizeZ,
                        new THREE.Vector3(-halfX - gridInset, -halfY, -halfZ),
                        'y',
                        'z',
                        0xd8d8d8
                      );
                      addContainerFaceGrid(
                        container.sizeX,
                        container.sizeY,
                        new THREE.Vector3(-halfX, -halfY, halfZ + gridInset),
                        'x',
                        'y',
                        0xd8d8d8
                      );
                      addContainerFaceGrid(
                        container.sizeX,
                        container.sizeZ,
                        new THREE.Vector3(-halfX, halfY + gridInset, -halfZ),
                        'x',
                        'z',
                        0xd8d8d8
                      );
                      addContainerFaceGrid(
                        container.sizeY,
                        container.sizeZ,
                        new THREE.Vector3(halfX + gridInset, -halfY, -halfZ),
                        'y',
                        'z',
                        0xd8d8d8
                      );

                      const axis = new THREE.AxesHelper(maxDim * 0.8);
                      scene.add(axis);

                      const itemList = document.getElementById('items');
                      placements.forEach((item, idx) => {
                        const color = new THREE.Color().setHSL((idx * 0.17) %% 1, 0.65, 0.55);
                        const geometry = new THREE.BoxGeometry(item.sizeX, item.sizeY, item.sizeZ);
                        const material = new THREE.MeshLambertMaterial({ color, transparent: true, opacity: 0.82 });
                        const mesh = new THREE.Mesh(geometry, material);

                        mesh.position.set(
                          -halfX + item.x + item.sizeX / 2,
                          -halfY + item.y + item.sizeY / 2,
                          -halfZ + item.z + item.sizeZ / 2
                        );
                        scene.add(mesh);

                        const edge = new THREE.LineSegments(
                          new THREE.EdgesGeometry(geometry),
                          new THREE.LineBasicMaterial({ color: 0x111111 })
                        );
                        edge.position.copy(mesh.position);
                        scene.add(edge);

                        const li = document.createElement('li');
                        const hex = '#' + color.getHexString();
                        li.innerHTML = `<span class="swatch" style="background:${hex}"></span>${item.id} origin=[${item.x},${item.y},${item.z}] size=[${item.sizeX},${item.sizeY},${item.sizeZ}]`;
                        itemList.appendChild(li);
                      });

                      function onResize() {
                        camera.aspect = window.innerWidth / window.innerHeight;
                        camera.updateProjectionMatrix();
                        renderer.setSize(window.innerWidth, window.innerHeight);
                        resizableLineMaterials.forEach(material => {
                          material.resolution.set(window.innerWidth, window.innerHeight);
                        });
                      }
                      window.addEventListener('resize', onResize);

                      function render() {
                        requestAnimationFrame(render);
                        renderer.render(scene, camera);
                      }
                      render();

                      tips.textContent = 'Mouse: rotate | wheel: zoom | right drag: pan';
                    } catch (error) {
                      tips.textContent = '3D render failed: ' + error.message;
                    }
                  </script>
                </body>
                </html>
                """.formatted(
                escapeHtml(caseId),
                escapeHtml(startedAt),
                escapeHtml(runTime),
                escapeHtml(summary),
                container.getSizeX(), container.getSizeY(), container.getSizeZ(),
                container.getSizeX(), container.getSizeY(), container.getSizeZ(),
                dataBuilder.toString()
        );
    }

    private String escapeJs(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
