/*
 * PIPELINE AUTOCONTENIDO - Caso Cineplanet
 * -----------------------------------------
 * No requiere repositorio en GitHub: el propio script escribe
 * los archivos Python en el workspace de Jenkins antes de ejecutarlos.
 *
 * CÓMO USARLO EN JENKINS:
 * 1. Jenkins → Nueva Tarea → nombre: "cineplanet-pipeline" → tipo: "Pipeline" → OK
 * 2. En la sección "Pipeline", en "Definition" selecciona: "Pipeline script"
 *    (NO "Pipeline script from SCM")
 * 3. Copia y pega TODO el contenido de este archivo en el cuadro de texto "Script"
 * 4. Guardar → "Construir ahora" (Build Now)
 */

pipeline {
    agent any

    environment {
        EQUIPO_RESPONSABLE = 'equipo-devops@cineplanet-demo.com'
        // IMPORTANTE: reemplaza esta ruta por la que obtuviste con "where python"
        // en tu Símbolo del sistema. Jenkins corre como servicio (LocalSystem) y
        // no ve tu PATH de usuario, por eso necesita la ruta completa.
        PYTHON_EXE = 'C:\\Users\\StivenOscar\\AppData\\Local\\Python\\bin\\python.exe'
    }

    stages {

        stage('Preparar workspace') {
            steps {
                echo 'Generando los archivos del proyecto en el workspace...'

                writeFile file: 'app.py', text: '''"""
Sistema simplificado de venta de entradas - Cineplanet.
Simula las funcionalidades críticas de la plataforma:
consulta de horarios, selección de asientos y confirmación de compra.
"""

import time
import random


class ErrorAsientoNoDisponible(Exception):
    pass


class ErrorCompraInvalida(Exception):
    pass


HORARIOS = {
    "AVENGERS_DOOMSDAY": ["14:00", "17:00", "20:00"],
    "ZOOTOPIA_2": ["15:30", "18:30"],
}

ASIENTOS = {f"A{i}": {"reservado": False, "confirmado": False} for i in range(1, 21)}


def consultar_horarios(pelicula):
    """P1: Consulta de horarios disponibles para una película."""
    if pelicula not in HORARIOS:
        return []
    return HORARIOS[pelicula]


def seleccionar_asiento(asiento_id):
    """P2: Selección/reserva temporal de un asiento."""
    asiento = ASIENTOS.get(asiento_id)
    if asiento is None:
        raise ErrorAsientoNoDisponible(f"El asiento {asiento_id} no existe")
    if asiento["reservado"]:
        raise ErrorAsientoNoDisponible(f"El asiento {asiento_id} ya está reservado")

    asiento["reservado"] = True
    return True


def confirmar_compra(asiento_id):
    """P3: Confirmación final de la compra de un asiento reservado."""
    asiento = ASIENTOS.get(asiento_id)
    if asiento is None:
        raise ErrorCompraInvalida(f"El asiento {asiento_id} no existe")
    if not asiento["reservado"]:
        raise ErrorCompraInvalida(
            f"No se puede confirmar la compra: el asiento {asiento_id} no fue reservado"
        )

    time.sleep(random.uniform(0.01, 0.05))
    asiento["confirmado"] = True
    return {"asiento": asiento_id, "estado": "CONFIRMADO"}


def reiniciar_asientos():
    """Utilidad para dejar el sistema en estado limpio entre pruebas."""
    for asiento in ASIENTOS.values():
        asiento["reservado"] = False
        asiento["confirmado"] = False
'''

                writeFile file: 'test_funcional.py', text: '''"""
Pruebas funcionales del sistema de venta de entradas.
Cada prueba está asociada a uno de los problemas identificados
en la plataforma de Cineplanet (P1, P2, P3).
"""

import pytest
from app import (
    consultar_horarios,
    seleccionar_asiento,
    confirmar_compra,
    reiniciar_asientos,
    ErrorAsientoNoDisponible,
    ErrorCompraInvalida,
)


@pytest.fixture(autouse=True)
def limpiar_estado():
    reiniciar_asientos()
    yield
    reiniciar_asientos()


def test_consultar_horarios_pelicula_valida():
    horarios = consultar_horarios("AVENGERS_DOOMSDAY")
    assert len(horarios) == 3
    assert "20:00" in horarios


def test_consultar_horarios_pelicula_no_existe():
    horarios = consultar_horarios("PELICULA_INEXISTENTE")
    assert horarios == []


def test_seleccionar_asiento_disponible():
    resultado = seleccionar_asiento("A1")
    assert resultado is True


def test_seleccionar_asiento_ya_reservado_lanza_error():
    seleccionar_asiento("A2")
    with pytest.raises(ErrorAsientoNoDisponible):
        seleccionar_asiento("A2")


def test_seleccionar_asiento_inexistente_lanza_error():
    with pytest.raises(ErrorAsientoNoDisponible):
        seleccionar_asiento("Z99")


def test_confirmar_compra_exitosa():
    seleccionar_asiento("A3")
    resultado = confirmar_compra("A3")
    assert resultado["estado"] == "CONFIRMADO"


def test_confirmar_compra_sin_reserva_lanza_error():
    with pytest.raises(ErrorCompraInvalida):
        confirmar_compra("A4")
'''

                writeFile file: 'test_rendimiento.py', text: '''"""
Prueba de rendimiento: simula el ingreso concurrente de múltiples usuarios
durante una preventa (P1), midiendo el tiempo total de respuesta del sistema.
"""

import time
from app import consultar_horarios, seleccionar_asiento, confirmar_compra, reiniciar_asientos

USUARIOS_SIMULADOS = 15
TIEMPO_MAXIMO_ACEPTABLE_SEGUNDOS = 3.0


def test_carga_concurrente_preventa():
    reiniciar_asientos()
    inicio = time.time()

    for i in range(1, USUARIOS_SIMULADOS + 1):
        consultar_horarios("AVENGERS_DOOMSDAY")
        asiento_id = f"A{i}"
        seleccionar_asiento(asiento_id)
        confirmar_compra(asiento_id)

    duracion = time.time() - inicio
    print(f"\\nTiempo total simulando {USUARIOS_SIMULADOS} usuarios: {duracion:.2f}s")

    assert duracion < TIEMPO_MAXIMO_ACEPTABLE_SEGUNDOS, (
        f"El sistema tardó {duracion:.2f}s, superando el límite de "
        f"{TIEMPO_MAXIMO_ACEPTABLE_SEGUNDOS}s"
    )
'''

                writeFile file: 'requirements.txt', text: 'pytest==8.3.3\n'

                echo 'Archivos generados correctamente en el workspace.'
            }
        }

        stage('Instalar dependencias') {
            steps {
                echo 'Instalando dependencias del proyecto...'
                bat "\"${PYTHON_EXE}\" -m pip install -r requirements.txt"
            }
        }

        stage('Pruebas funcionales') {
            steps {
                echo 'Ejecutando pruebas funcionales sobre horarios, asientos y compras (P1, P2, P3)...'
                bat "\"${PYTHON_EXE}\" -m pytest test_funcional.py -v"
            }
        }

        stage('Pruebas de rendimiento') {
            steps {
                echo 'Simulando ingreso concurrente de usuarios durante preventa (P1)...'
                bat "\"${PYTHON_EXE}\" -m pytest test_rendimiento.py -v -s"
            }
        }

        stage('Despliegue') {
            steps {
                echo 'Todas las pruebas pasaron. Desplegando nueva versión a producción...'
            }
        }
    }

    post {
        success {
            echo 'Pipeline ejecutado correctamente. La versión es apta para producción.'
            // mail to: "${EQUIPO_RESPONSABLE}",
            //      subject: "Jenkins: Build EXITOSO - ${env.JOB_NAME} #${env.BUILD_NUMBER}",
            //      body: "Todas las pruebas funcionales y de rendimiento pasaron correctamente."
        }
        failure {
            echo 'El pipeline falló. Se detiene el despliegue para evitar una versión defectuosa en producción.'
            // mail to: "${EQUIPO_RESPONSABLE}",
            //      subject: "Jenkins: Build FALLIDO - ${env.JOB_NAME} #${env.BUILD_NUMBER}",
            //      body: "Una o más pruebas fallaron. Revisar: ${env.BUILD_URL}"
        }
    }
}
